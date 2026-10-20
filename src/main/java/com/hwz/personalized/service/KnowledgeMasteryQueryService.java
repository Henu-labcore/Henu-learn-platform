package com.hwz.personalized.service;

import com.hwz.common.entity.User;
import com.hwz.common.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生知识点掌握度查询服务。
 *
 * 用于向管理员端返回：
 * 1. 学生基本信息；
 * 2. 掌握度汇总数据；
 * 3. 每个知识点的详细得分和风险等级。
 */
@Service
public class KnowledgeMasteryQueryService {

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    private final JdbcTemplate jdbcTemplate;

    private final UserMapper userMapper;

    public KnowledgeMasteryQueryService(
            JdbcTemplate jdbcTemplate,
            UserMapper userMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
    }

    /**
     * 查询指定学生的掌握度详情。
     */
    public Map<String, Object> getStudentMastery(
            Long studentId) {
        validateStudent(studentId);

        Map<String, Object> student =
                queryStudent(studentId);

        List<Map<String, Object>> masteryList =
                queryMasteryList(studentId);

        Map<String, Object> summary =
                buildSummary(masteryList);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "student",
                student
        );

        response.put(
                "summary",
                summary
        );

        response.put(
                "count",
                masteryList.size()
        );

        response.put(
                "data",
                masteryList
        );

        return response;
    }

    /**
     * 查询学生基础信息。
     */
    private Map<String, Object> queryStudent(
            Long studentId) {
        String sql =
                "SELECT "
                        + "user_id, "
                        + "username, "
                        + "display_name, "
                        + "real_name, "
                        + "role, "
                        + "status "
                        + "FROM sys_user "
                        + "WHERE user_id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                (resultSet, rowNumber) -> {
                    Map<String, Object> student =
                            new LinkedHashMap<>();

                    student.put(
                            "studentId",
                            resultSet.getLong(
                                    "user_id"
                            )
                    );

                    student.put(
                            "username",
                            resultSet.getString(
                                    "username"
                            )
                    );

                    student.put(
                            "displayName",
                            resultSet.getString(
                                    "display_name"
                            )
                    );

                    student.put(
                            "realName",
                            resultSet.getString(
                                    "real_name"
                            )
                    );

                    student.put(
                            "role",
                            resultSet.getString(
                                    "role"
                            )
                    );

                    student.put(
                            "status",
                            resultSet.getString(
                                    "status"
                            )
                    );

                    return student;
                },
                studentId
        );
    }

    /**
     * 查询学生每个知识点的掌握度详情。
     */
    private List<Map<String, Object>> queryMasteryList(
            Long studentId) {
        String sql =
                "SELECT "
                        + "skm.mastery_id, "
                        + "skm.student_id, "
                        + "kp.knowledge_point_id, "
                        + "kp.code, "
                        + "kp.name AS knowledge_point_name, "
                        + "kp.description, "
                        + "kp.category, "
                        + "kp.difficulty, "
                        + "kp.warning_threshold, "
                        + "skm.learning_score, "
                        + "skm.assessment_score, "
                        + "skm.mastery_score, "
                        + "skm.risk_level, "
                        + "skm.completed_learning_count, "
                        + "skm.total_learning_count, "
                        + "skm.graded_assignment_count, "
                        + "skm.total_assignment_count, "
                        + "skm.last_learning_time, "
                        + "skm.last_assessment_time, "
                        + "skm.calculated_at "
                        + "FROM student_knowledge_mastery skm "
                        + "INNER JOIN knowledge_point kp "
                        + "    ON kp.knowledge_point_id = "
                        + "       skm.knowledge_point_id "
                        + "WHERE skm.student_id = ? "
                        + "ORDER BY "
                        + "    kp.sort_order, "
                        + "    kp.knowledge_point_id";

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> {
                    Map<String, Object> row =
                            new LinkedHashMap<>();

                    row.put(
                            "masteryId",
                            resultSet.getLong(
                                    "mastery_id"
                            )
                    );

                    row.put(
                            "studentId",
                            resultSet.getLong(
                                    "student_id"
                            )
                    );

                    row.put(
                            "knowledgePointId",
                            resultSet.getLong(
                                    "knowledge_point_id"
                            )
                    );

                    row.put(
                            "code",
                            resultSet.getString(
                                    "code"
                            )
                    );

                    row.put(
                            "knowledgePointName",
                            resultSet.getString(
                                    "knowledge_point_name"
                            )
                    );

                    row.put(
                            "description",
                            resultSet.getString(
                                    "description"
                            )
                    );

                    row.put(
                            "category",
                            resultSet.getString(
                                    "category"
                            )
                    );

                    row.put(
                            "difficulty",
                            resultSet.getString(
                                    "difficulty"
                            )
                    );

                    row.put(
                            "warningThreshold",
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "warning_threshold"
                                    )
                            )
                    );

                    row.put(
                            "learningScore",
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "learning_score"
                                    )
                            )
                    );

                    row.put(
                            "assessmentScore",
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "assessment_score"
                                    )
                            )
                    );

                    row.put(
                            "masteryScore",
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "mastery_score"
                                    )
                            )
                    );

                    row.put(
                            "riskLevel",
                            resultSet.getString(
                                    "risk_level"
                            )
                    );

                    row.put(
                            "completedLearningCount",
                            resultSet.getInt(
                                    "completed_learning_count"
                            )
                    );

                    row.put(
                            "totalLearningCount",
                            resultSet.getInt(
                                    "total_learning_count"
                            )
                    );

                    row.put(
                            "gradedAssignmentCount",
                            resultSet.getInt(
                                    "graded_assignment_count"
                            )
                    );

                    row.put(
                            "totalAssignmentCount",
                            resultSet.getInt(
                                    "total_assignment_count"
                            )
                    );

                    row.put(
                            "lastLearningTime",
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "last_learning_time"
                                    )
                            )
                    );

                    row.put(
                            "lastAssessmentTime",
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "last_assessment_time"
                                    )
                            )
                    );

                    row.put(
                            "calculatedAt",
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "calculated_at"
                                    )
                            )
                    );

                    return row;
                },
                studentId
        );
    }

    /**
     * 根据知识点明细生成汇总统计。
     */
    private Map<String, Object> buildSummary(
            List<Map<String, Object>> masteryList) {
        int totalKnowledgePointCount =
                masteryList.size();

        int highRiskCount = 0;
        int mediumRiskCount = 0;
        int lowRiskCount = 0;
        int unknownRiskCount = 0;

        BigDecimal totalMasteryScore =
                ZERO;

        List<String> weakKnowledgePointNames =
                new ArrayList<>();

        for (Map<String, Object> item
                : masteryList) {
            String riskLevel =
                    String.valueOf(
                            item.get("riskLevel")
                    );

            BigDecimal masteryScore =
                    safeDecimal(
                            (BigDecimal) item.get(
                                    "masteryScore"
                            )
                    );

            totalMasteryScore =
                    totalMasteryScore.add(
                            masteryScore
                    );

            if ("HIGH".equals(riskLevel)) {
                highRiskCount++;

                weakKnowledgePointNames.add(
                        String.valueOf(
                                item.get(
                                        "knowledgePointName"
                                )
                        )
                );

            } else if ("MEDIUM".equals(
                    riskLevel)) {
                mediumRiskCount++;

            } else if ("LOW".equals(
                    riskLevel)) {
                lowRiskCount++;

            } else {
                unknownRiskCount++;
            }
        }

        BigDecimal averageMasteryScore =
                ZERO;

        if (totalKnowledgePointCount > 0) {
            averageMasteryScore =
                    totalMasteryScore.divide(
                            BigDecimal.valueOf(
                                    totalKnowledgePointCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        Map<String, Object> summary =
                new LinkedHashMap<>();

        summary.put(
                "totalKnowledgePointCount",
                totalKnowledgePointCount
        );

        summary.put(
                "highRiskCount",
                highRiskCount
        );

        summary.put(
                "mediumRiskCount",
                mediumRiskCount
        );

        summary.put(
                "lowRiskCount",
                lowRiskCount
        );

        summary.put(
                "unknownRiskCount",
                unknownRiskCount
        );

        summary.put(
                "averageMasteryScore",
                averageMasteryScore
        );

        summary.put(
                "weakKnowledgePointNames",
                weakKnowledgePointNames
        );

        return summary;
    }

    /**
     * 验证目标用户是启用状态的普通用户。
     */
    private void validateStudent(
            Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student ID is required"
            );
        }

        User student =
                userMapper.selectById(
                        studentId
                );

        if (student == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student not found"
            );
        }

        if (student.getRole()
                != User.Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Target user is not a student"
            );
        }

        if (student.getStatus()
                != User.Status.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Target student is not active"
            );
        }
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