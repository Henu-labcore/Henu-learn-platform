package com.hwz.personalized.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端学生知识点风险总览服务。
 *
 * 用于返回：
 * 1. 启用状态的普通用户列表；
 * 2. 每名学生的掌握度和风险统计；
 * 3. 全体学生的风险汇总。
 */
@Service
public class KnowledgeMasteryOverviewService {

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeMasteryOverviewService(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询全部启用学生的掌握度总览。
     */
    public Map<String, Object> getStudentOverview() {
        List<Map<String, Object>> students =
                queryStudentOverview();

        Map<String, Object> summary =
                buildSummary(students);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "summary",
                summary
        );

        response.put(
                "count",
                students.size()
        );

        response.put(
                "data",
                students
        );

        return response;
    }

    /**
     * 查询每名启用学生的掌握度统计。
     *
     * 即使学生尚未计算掌握度，也会出现在结果中。
     */
    private List<Map<String, Object>>
    queryStudentOverview() {
        String sql =
                "SELECT "
                        + "u.user_id, "
                        + "u.username, "
                        + "u.display_name, "
                        + "u.real_name, "
                        + "u.status, "

                        + "COUNT(skm.mastery_id) "
                        + "    AS knowledge_point_count, "

                        + "COALESCE( "
                        + "    ROUND(AVG(skm.mastery_score), 2), "
                        + "    0.00 "
                        + ") AS average_mastery_score, "

                        + "SUM( "
                        + "    CASE "
                        + "        WHEN skm.risk_level = 'HIGH' "
                        + "        THEN 1 "
                        + "        ELSE 0 "
                        + "    END "
                        + ") AS high_risk_count, "

                        + "SUM( "
                        + "    CASE "
                        + "        WHEN skm.risk_level = 'MEDIUM' "
                        + "        THEN 1 "
                        + "        ELSE 0 "
                        + "    END "
                        + ") AS medium_risk_count, "

                        + "SUM( "
                        + "    CASE "
                        + "        WHEN skm.risk_level = 'LOW' "
                        + "        THEN 1 "
                        + "        ELSE 0 "
                        + "    END "
                        + ") AS low_risk_count, "

                        + "SUM( "
                        + "    CASE "
                        + "        WHEN skm.risk_level = 'UNKNOWN' "
                        + "        THEN 1 "
                        + "        ELSE 0 "
                        + "    END "
                        + ") AS unknown_risk_count, "

                        + "MAX(skm.calculated_at) "
                        + "    AS last_calculated_at "

                        + "FROM sys_user u "

                        + "LEFT JOIN student_knowledge_mastery skm "
                        + "    ON skm.student_id = u.user_id "

                        + "WHERE u.role = 'USER' "
                        + "  AND u.status = 'ACTIVE' "

                        + "GROUP BY "
                        + "    u.user_id, "
                        + "    u.username, "
                        + "    u.display_name, "
                        + "    u.real_name, "
                        + "    u.status "

                        + "ORDER BY "
                        + "    high_risk_count DESC, "
                        + "    average_mastery_score ASC, "
                        + "    u.user_id ASC";

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> {
                    Map<String, Object> row =
                            new LinkedHashMap<>();

                    int knowledgePointCount =
                            resultSet.getInt(
                                    "knowledge_point_count"
                            );

                    int highRiskCount =
                            resultSet.getInt(
                                    "high_risk_count"
                            );

                    int mediumRiskCount =
                            resultSet.getInt(
                                    "medium_risk_count"
                            );

                    int lowRiskCount =
                            resultSet.getInt(
                                    "low_risk_count"
                            );

                    int unknownRiskCount =
                            resultSet.getInt(
                                    "unknown_risk_count"
                            );

                    row.put(
                            "studentId",
                            resultSet.getLong(
                                    "user_id"
                            )
                    );

                    row.put(
                            "username",
                            resultSet.getString(
                                    "username"
                            )
                    );

                    row.put(
                            "displayName",
                            resultSet.getString(
                                    "display_name"
                            )
                    );

                    row.put(
                            "realName",
                            resultSet.getString(
                                    "real_name"
                            )
                    );

                    row.put(
                            "status",
                            resultSet.getString(
                                    "status"
                            )
                    );

                    row.put(
                            "knowledgePointCount",
                            knowledgePointCount
                    );

                    row.put(
                            "averageMasteryScore",
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "average_mastery_score"
                                    )
                            )
                    );

                    row.put(
                            "highRiskCount",
                            highRiskCount
                    );

                    row.put(
                            "mediumRiskCount",
                            mediumRiskCount
                    );

                    row.put(
                            "lowRiskCount",
                            lowRiskCount
                    );

                    row.put(
                            "unknownRiskCount",
                            unknownRiskCount
                    );

                    row.put(
                            "masteryStatus",
                            knowledgePointCount > 0
                                    ? "CALCULATED"
                                    : "NOT_CALCULATED"
                    );

                    row.put(
                            "highestRiskLevel",
                            calculateHighestRiskLevel(
                                    knowledgePointCount,
                                    highRiskCount,
                                    mediumRiskCount,
                                    lowRiskCount
                            )
                    );

                    row.put(
                            "lastCalculatedAt",
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "last_calculated_at"
                                    )
                            )
                    );

                    return row;
                }
        );
    }

    /**
     * 生成全体学生汇总统计。
     */
    private Map<String, Object> buildSummary(
            List<Map<String, Object>> students) {
        int activeStudentCount =
                students.size();

        int calculatedStudentCount = 0;
        int uncalculatedStudentCount = 0;
        int highRiskStudentCount = 0;

        int totalKnowledgePointCount = 0;
        int totalHighRiskCount = 0;
        int totalMediumRiskCount = 0;
        int totalLowRiskCount = 0;
        int totalUnknownRiskCount = 0;

        BigDecimal calculatedStudentScoreSum =
                ZERO;

        List<String> highRiskStudentNames =
                new ArrayList<>();

        for (Map<String, Object> student
                : students) {
            int knowledgePointCount =
                    (Integer) student.get(
                            "knowledgePointCount"
                    );

            int highRiskCount =
                    (Integer) student.get(
                            "highRiskCount"
                    );

            int mediumRiskCount =
                    (Integer) student.get(
                            "mediumRiskCount"
                    );

            int lowRiskCount =
                    (Integer) student.get(
                            "lowRiskCount"
                    );

            int unknownRiskCount =
                    (Integer) student.get(
                            "unknownRiskCount"
                    );

            totalKnowledgePointCount +=
                    knowledgePointCount;

            totalHighRiskCount +=
                    highRiskCount;

            totalMediumRiskCount +=
                    mediumRiskCount;

            totalLowRiskCount +=
                    lowRiskCount;

            totalUnknownRiskCount +=
                    unknownRiskCount;

            if (knowledgePointCount > 0) {
                calculatedStudentCount++;

                calculatedStudentScoreSum =
                        calculatedStudentScoreSum.add(
                                safeDecimal(
                                        (BigDecimal) student.get(
                                                "averageMasteryScore"
                                        )
                                )
                        );
            } else {
                uncalculatedStudentCount++;
            }

            if (highRiskCount > 0) {
                highRiskStudentCount++;

                highRiskStudentNames.add(
                        getStudentDisplayName(
                                student
                        )
                );
            }
        }

        BigDecimal averageMasteryScore =
                ZERO;

        if (calculatedStudentCount > 0) {
            averageMasteryScore =
                    calculatedStudentScoreSum.divide(
                            BigDecimal.valueOf(
                                    calculatedStudentCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        Map<String, Object> summary =
                new LinkedHashMap<>();

        summary.put(
                "activeStudentCount",
                activeStudentCount
        );

        summary.put(
                "calculatedStudentCount",
                calculatedStudentCount
        );

        summary.put(
                "uncalculatedStudentCount",
                uncalculatedStudentCount
        );

        summary.put(
                "highRiskStudentCount",
                highRiskStudentCount
        );

        summary.put(
                "totalKnowledgePointCount",
                totalKnowledgePointCount
        );

        summary.put(
                "totalHighRiskCount",
                totalHighRiskCount
        );

        summary.put(
                "totalMediumRiskCount",
                totalMediumRiskCount
        );

        summary.put(
                "totalLowRiskCount",
                totalLowRiskCount
        );

        summary.put(
                "totalUnknownRiskCount",
                totalUnknownRiskCount
        );

        summary.put(
                "averageMasteryScore",
                averageMasteryScore
        );

        summary.put(
                "highRiskStudentNames",
                highRiskStudentNames
        );

        return summary;
    }

    /**
     * 计算学生当前最高风险等级。
     */
    private String calculateHighestRiskLevel(
            int knowledgePointCount,
            int highRiskCount,
            int mediumRiskCount,
            int lowRiskCount) {
        if (knowledgePointCount <= 0) {
            return "NOT_CALCULATED";
        }

        if (highRiskCount > 0) {
            return "HIGH";
        }

        if (mediumRiskCount > 0) {
            return "MEDIUM";
        }

        if (lowRiskCount > 0) {
            return "LOW";
        }

        return "UNKNOWN";
    }

    /**
     * 获取学生展示名称。
     */
    private String getStudentDisplayName(
            Map<String, Object> student) {
        Object displayName =
                student.get(
                        "displayName"
                );

        if (displayName != null
                && !String.valueOf(displayName)
                .trim()
                .isEmpty()) {
            return String.valueOf(
                    displayName
            );
        }

        Object realName =
                student.get(
                        "realName"
                );

        if (realName != null
                && !String.valueOf(realName)
                .trim()
                .isEmpty()) {
            return String.valueOf(
                    realName
            );
        }

        return String.valueOf(
                student.get("username")
        );
    }

    private BigDecimal safeDecimal(
            BigDecimal value) {
        if (value == null) {
            return ZERO;
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private LocalDateTime toLocalDateTime(
            Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }
}