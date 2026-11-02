package com.hwz.personalized.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.common.entity.KnowledgePoint;
import com.hwz.common.entity.StudentKnowledgeMastery;
import com.hwz.common.mapper.KnowledgePointMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 掌握度历史快照的写入与可视化查询。
 *
 * <p>写入：每次掌握度重算后调用 {@link #recordSnapshot}，
 * 同一学生/知识点/同一天只保留最后一次（唯一键 + ON DUPLICATE KEY UPDATE）。</p>
 *
 * <p>查询：
 * <ul>
 *   <li>学生端：知识点 × 日期 热力图 + 个人掌握度趋势；</li>
 *   <li>管理端：学生 × 知识点 矩阵热力图 + 班级掌握度趋势。</li>
 * </ul></p>
 */
@Service
public class MasteryHistoryService {

    private static final int DEFAULT_DAYS = 30;

    private static final int MAX_DAYS = 180;

    /** 热力图每页最多显示的学生数（行数）。 */
    private static final int MATRIX_PAGE_SIZE = 8;

    /** 搜索关键词最大长度。 */
    private static final int MAX_KEYWORD_LENGTH = 50;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String UNKNOWN = "UNKNOWN";

    private final JdbcTemplate jdbcTemplate;

    private final KnowledgePointMapper knowledgePointMapper;

    public MasteryHistoryService(
            JdbcTemplate jdbcTemplate,
            KnowledgePointMapper knowledgePointMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.knowledgePointMapper = knowledgePointMapper;
    }

    /**
     * 写入一条当日掌握度历史。
     *
     * <p>同一天重复写入会覆盖为最新值，因此学生反复打开页面
     * 也只会产生一条当天记录。</p>
     */
    public void recordSnapshot(
            StudentKnowledgeMastery mastery,
            LocalDate snapshotDate) {

        if (mastery == null
                || mastery.getStudentId() == null
                || mastery.getKnowledgePointId() == null) {
            return;
        }

        LocalDate date = snapshotDate == null
                ? LocalDate.now()
                : snapshotDate;

        jdbcTemplate.update(
                "INSERT INTO student_mastery_history ("
                        + " student_id, knowledge_point_id,"
                        + " learning_score, assessment_score,"
                        + " mastery_score, risk_level,"
                        + " snapshot_date, calculated_at, created_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON DUPLICATE KEY UPDATE"
                        + " learning_score = VALUES(learning_score),"
                        + " assessment_score = VALUES(assessment_score),"
                        + " mastery_score = VALUES(mastery_score),"
                        + " risk_level = VALUES(risk_level),"
                        + " calculated_at = VALUES(calculated_at)",
                mastery.getStudentId(),
                mastery.getKnowledgePointId(),
                safeDecimal(mastery.getLearningScore()),
                safeDecimal(mastery.getAssessmentScore()),
                safeDecimal(mastery.getMasteryScore()),
                mastery.getRiskLevel() == null
                        ? UNKNOWN
                        : mastery.getRiskLevel(),
                Date.valueOf(date),
                mastery.getCalculatedAt() == null
                        ? LocalDateTime.now()
                        : mastery.getCalculatedAt(),
                LocalDateTime.now()
        );
    }

    /**
     * 学生端：个人掌握度历史（热力图 + 趋势）。
     *
     * @param studentId 学生ID
     * @param days      最近天数（1~180，默认 30）
     */
    public Map<String, Object> getStudentHistory(
            Long studentId,
            Integer days) {

        int resolvedDays = normalizeDays(days);

        LocalDate endDate = LocalDate.now();

        LocalDate startDate =
                endDate.minusDays(resolvedDays - 1L);

        List<KnowledgePoint> knowledgePoints =
                listActiveKnowledgePoints();

        List<LocalDate> dates =
                buildDateRange(startDate, endDate);

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT knowledge_point_id,"
                                + " snapshot_date,"
                                + " learning_score,"
                                + " assessment_score,"
                                + " mastery_score,"
                                + " risk_level"
                                + " FROM student_mastery_history"
                                + " WHERE student_id = ?"
                                + " AND snapshot_date >= ?"
                                + " ORDER BY snapshot_date, knowledge_point_id",
                        studentId,
                        Date.valueOf(startDate)
                );

        List<Map<String, Object>> cells =
                new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put(
                    "knowledgePointId",
                    toLong(row.get("knowledge_point_id"))
            );
            cell.put(
                    "date",
                    toDateString(row.get("snapshot_date"))
            );
            cell.put(
                    "learningScore",
                    toDecimal(row.get("learning_score"))
            );
            cell.put(
                    "assessmentScore",
                    toDecimal(row.get("assessment_score"))
            );
            cell.put(
                    "masteryScore",
                    toDecimal(row.get("mastery_score"))
            );
            cell.put(
                    "riskLevel",
                    row.get("risk_level")
            );
            cells.add(cell);
        }

        List<Map<String, Object>> trend =
                buildStudentTrend(dates, cells);

        Map<String, Object> response =
                new LinkedHashMap<>();
        response.put("studentId", studentId);
        response.put("days", resolvedDays);
        response.put(
                "startDate",
                startDate.format(DATE_FORMAT)
        );
        response.put(
                "endDate",
                endDate.format(DATE_FORMAT)
        );
        response.put(
                "dates",
                dates.stream()
                        .map(date -> date.format(DATE_FORMAT))
                        .collect(java.util.stream.Collectors.toList())
        );
        response.put(
                "knowledgePoints",
                toKnowledgePointViews(knowledgePoints)
        );
        response.put("cells", cells);
        response.put("trend", trend);

        return response;
    }

    /**
     * 管理端：学生 × 知识点 当前掌握度矩阵（用于热力图）。
     *
     * <p>按"高风险数降序、平均分升序"排序后分页，每页最多 8 名学生，
     * 避免一次渲染过多行导致热力图不可读；支持按姓名/学号/用户名模糊搜索。</p>
     *
     * @param keyword  模糊搜索词，可为空
     * @param page     页码，从 1 开始
     * @param pageSize 每页人数（1~8，默认 8）
     */
    public Map<String, Object> getAdminMatrix(
            String keyword,
            Integer page,
            Integer pageSize) {

        String searchKeyword =
                normalizeKeyword(keyword);

        int resolvedPageSize =
                normalizePageSize(pageSize);

        int resolvedPage =
                normalizePage(page);

        List<KnowledgePoint> knowledgePoints =
                listActiveKnowledgePoints();

        boolean hasKeyword =
                !searchKeyword.isEmpty();

        String keywordClause = hasKeyword
                ? " AND (u.username LIKE ?"
                + " OR u.display_name LIKE ?"
                + " OR u.real_name LIKE ?)"
                : "";

        Object[] keywordArgs = hasKeyword
                ? new Object[]{
                        "%" + searchKeyword + "%",
                        "%" + searchKeyword + "%",
                        "%" + searchKeyword + "%"}
                : new Object[0];

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user u"
                        + " WHERE u.role = 'USER'"
                        + " AND u.status = 'ACTIVE'"
                        + keywordClause,
                Long.class,
                keywordArgs
        );

        long totalCount =
                total == null ? 0L : total;

        int totalPages = totalCount == 0
                ? 0
                : (int) Math.ceil(
                        totalCount * 1.0
                                / resolvedPageSize
                );

        if (totalPages > 0
                && resolvedPage > totalPages) {
            resolvedPage = totalPages;
        }

        int offset =
                (resolvedPage - 1) * resolvedPageSize;

        Object[] listArgs = hasKeyword
                ? new Object[]{
                        keywordArgs[0],
                        keywordArgs[1],
                        keywordArgs[2],
                        resolvedPageSize,
                        offset}
                : new Object[]{
                        resolvedPageSize,
                        offset};

        List<Map<String, Object>> students =
                jdbcTemplate.queryForList(
                        "SELECT u.user_id, u.username,"
                                + " u.display_name, u.real_name,"
                                + " COUNT(skm.mastery_id) AS mastery_count,"
                                + " ROUND(AVG(skm.mastery_score), 2) AS average_score,"
                                + " COALESCE(SUM(skm.risk_level = 'HIGH'), 0) AS high_count"
                                + " FROM sys_user u"
                                + " LEFT JOIN student_knowledge_mastery skm"
                                + " ON skm.student_id = u.user_id"
                                + " WHERE u.role = 'USER'"
                                + " AND u.status = 'ACTIVE'"
                                + keywordClause
                                + " GROUP BY u.user_id, u.username,"
                                + " u.display_name, u.real_name"
                                + " ORDER BY high_count DESC,"
                                + " average_score ASC, u.user_id ASC"
                                + " LIMIT ? OFFSET ?",
                        listArgs
                );

        List<Map<String, Object>> studentViews =
                new ArrayList<>();

        List<Long> studentIds =
                new ArrayList<>();

        for (Map<String, Object> row : students) {
            Map<String, Object> student =
                    new LinkedHashMap<>();

            Long studentId =
                    toLong(row.get("user_id"));

            student.put(
                    "studentId",
                    studentId
            );
            student.put("username", row.get("username"));
            student.put(
                    "displayName",
                    row.get("display_name")
            );
            student.put("realName", row.get("real_name"));
            student.put(
                    "masteryCount",
                    toInt(row.get("mastery_count"))
            );
            student.put(
                    "averageMasteryScore",
                    toDecimal(row.get("average_score"))
            );
            student.put(
                    "highRiskCount",
                    toInt(row.get("high_count"))
            );

            if (studentId != null) {
                studentIds.add(studentId);
            }

            studentViews.add(student);
        }

        List<Map<String, Object>> cells =
                new ArrayList<>();

        if (!studentIds.isEmpty()) {
            StringBuilder placeholders =
                    new StringBuilder();

            for (int i = 0; i < studentIds.size(); i++) {
                if (i > 0) {
                    placeholders.append(", ");
                }
                placeholders.append("?");
            }

            cells = jdbcTemplate.queryForList(
                    "SELECT skm.student_id,"
                            + " skm.knowledge_point_id,"
                            + " skm.mastery_score,"
                            + " skm.risk_level"
                            + " FROM student_knowledge_mastery skm"
                            + " WHERE skm.student_id IN ("
                            + placeholders
                            + ")",
                    studentIds.toArray()
            );
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("page", resolvedPage);
        response.put("pageSize", resolvedPageSize);
        response.put("total", totalCount);
        response.put("totalPages", totalPages);
        response.put("keyword", searchKeyword);
        response.put(
                "knowledgePoints",
                toKnowledgePointViews(knowledgePoints)
        );
        response.put("students", studentViews);
        response.put(
                "cells",
                cells.stream().map(row -> {
                    Map<String, Object> cell =
                            new LinkedHashMap<>();
                    cell.put(
                            "studentId",
                            toLong(row.get("student_id"))
                    );
                    cell.put(
                            "knowledgePointId",
                            toLong(row.get("knowledge_point_id"))
                    );
                    cell.put(
                            "masteryScore",
                            toDecimal(row.get("mastery_score"))
                    );
                    cell.put(
                            "riskLevel",
                            row.get("risk_level")
                    );
                    return cell;
                }).collect(
                        java.util.stream.Collectors.toList()
                )
        );

        return response;
    }

    /**
     * 管理端：班级掌握度趋势（按天，仅整体平均线）。
     */
    public Map<String, Object> getAdminTrend(Integer days) {

        int resolvedDays = normalizeDays(days);

        LocalDate endDate = LocalDate.now();

        LocalDate startDate =
                endDate.minusDays(resolvedDays - 1L);

        List<LocalDate> dates =
                buildDateRange(startDate, endDate);

        List<Map<String, Object>> aggregated =
                jdbcTemplate.queryForList(
                        "SELECT h.snapshot_date,"
                                + " ROUND(AVG(CASE WHEN h.risk_level <> 'UNKNOWN'"
                                + " THEN h.mastery_score END), 2) AS average_score,"
                                + " COUNT(DISTINCT CASE WHEN h.risk_level <> 'UNKNOWN'"
                                + " THEN h.student_id END) AS contacted_students,"
                                + " COALESCE(SUM(h.risk_level = 'HIGH'), 0) AS high_count,"
                                + " COALESCE(SUM(h.risk_level = 'MEDIUM'), 0) AS medium_count,"
                                + " COALESCE(SUM(h.risk_level = 'LOW'), 0) AS low_count,"
                                + " COALESCE(SUM(h.risk_level = 'UNKNOWN'), 0) AS unknown_count"
                                + " FROM student_mastery_history h"
                                + " JOIN sys_user u ON u.user_id = h.student_id"
                                + " WHERE u.role = 'USER' AND u.status = 'ACTIVE'"
                                + " AND h.snapshot_date >= ?"
                                + " GROUP BY h.snapshot_date"
                                + " ORDER BY h.snapshot_date",
                        Date.valueOf(startDate)
                );

        Map<String, Map<String, Object>> aggregatedByDate =
                new LinkedHashMap<>();

        for (Map<String, Object> row : aggregated) {
            aggregatedByDate.put(
                    toDateString(row.get("snapshot_date")),
                    row
            );
        }

        List<Map<String, Object>> trend =
                new ArrayList<>();

        for (LocalDate date : dates) {
            String key = date.format(DATE_FORMAT);
            Map<String, Object> row =
                    aggregatedByDate.get(key);

            Map<String, Object> point =
                    new LinkedHashMap<>();
            point.put("date", key);
            point.put(
                    "averageMasteryScore",
                    row == null
                            ? null
                            : toDecimal(row.get("average_score"))
            );
            point.put(
                    "contactedStudentCount",
                    row == null
                            ? 0
                            : toInt(row.get("contacted_students"))
            );
            point.put(
                    "highRiskCount",
                    row == null
                            ? 0
                            : toInt(row.get("high_count"))
            );
            point.put(
                    "mediumRiskCount",
                    row == null
                            ? 0
                            : toInt(row.get("medium_count"))
            );
            point.put(
                    "lowRiskCount",
                    row == null
                            ? 0
                            : toInt(row.get("low_count"))
            );
            point.put(
                    "unknownRiskCount",
                    row == null
                            ? 0
                            : toInt(row.get("unknown_count"))
            );
            trend.add(point);
        }

        Map<String, Object> response =
                new LinkedHashMap<>();
        response.put("days", resolvedDays);
        response.put(
                "startDate",
                startDate.format(DATE_FORMAT)
        );
        response.put(
                "endDate",
                endDate.format(DATE_FORMAT)
        );
        response.put(
                "dates",
                dates.stream()
                        .map(date -> date.format(DATE_FORMAT))
                        .collect(java.util.stream.Collectors.toList())
        );
        response.put("trend", trend);

        return response;
    }

    /**
     * 学生个人趋势：按天聚合，只统计"已接触"知识点，
     * 避免新增知识点时把平均值拉低造成视觉跳变。
     */
    private List<Map<String, Object>> buildStudentTrend(
            List<LocalDate> dates,
            List<Map<String, Object>> cells) {

        Map<String, List<Map<String, Object>>> cellsByDate =
                new LinkedHashMap<>();

        for (Map<String, Object> cell : cells) {
            String date = String.valueOf(cell.get("date"));
            cellsByDate
                    .computeIfAbsent(date, key -> new ArrayList<>())
                    .add(cell);
        }

        List<Map<String, Object>> trend =
                new ArrayList<>();

        for (LocalDate date : dates) {
            String key = date.format(DATE_FORMAT);
            List<Map<String, Object>> dayCells =
                    cellsByDate.get(key);

            long contacted = 0;
            BigDecimal sum = BigDecimal.ZERO;
            int high = 0;
            int medium = 0;
            int low = 0;
            int unknown = 0;

            if (dayCells != null) {
                for (Map<String, Object> cell : dayCells) {
                    String risk = String.valueOf(
                            cell.get("riskLevel")
                    );

                    if (UNKNOWN.equals(risk)) {
                        unknown++;
                        continue;
                    }

                    contacted++;
                    sum = sum.add(
                            safeDecimal(
                                    (BigDecimal) cell.get(
                                            "masteryScore"
                                    )
                            )
                    );

                    if ("HIGH".equals(risk)) {
                        high++;
                    } else if ("MEDIUM".equals(risk)) {
                        medium++;
                    } else if ("LOW".equals(risk)) {
                        low++;
                    }
                }
            }

            Map<String, Object> point =
                    new LinkedHashMap<>();
            point.put("date", key);
            point.put(
                    "averageMasteryScore",
                    contacted == 0
                            ? null
                            : sum.divide(
                                    BigDecimal.valueOf(contacted),
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );
            point.put(
                    "contactedKnowledgePointCount",
                    contacted
            );
            point.put("highRiskCount", high);
            point.put("mediumRiskCount", medium);
            point.put("lowRiskCount", low);
            point.put("unknownRiskCount", unknown);
            trend.add(point);
        }

        return trend;
    }

    private List<KnowledgePoint> listActiveKnowledgePoints() {
        return knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(
                                KnowledgePoint::getStatus,
                                "ACTIVE"
                        )
                        .orderByAsc(
                                KnowledgePoint::getSortOrder
                        )
                        .orderByAsc(
                                KnowledgePoint::getKnowledgePointId
                        )
        );
    }

    private List<Map<String, Object>> toKnowledgePointViews(
            List<KnowledgePoint> knowledgePoints) {

        List<Map<String, Object>> views =
                new ArrayList<>();

        for (KnowledgePoint knowledgePoint : knowledgePoints) {
            Map<String, Object> view =
                    new LinkedHashMap<>();
            view.put(
                    "knowledgePointId",
                    knowledgePoint.getKnowledgePointId()
            );
            view.put("code", knowledgePoint.getCode());
            view.put("name", knowledgePoint.getName());
            view.put(
                    "warningThreshold",
                    knowledgePoint.getWarningThreshold()
            );
            views.add(view);
        }

        return views;
    }

    private List<LocalDate> buildDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        List<LocalDate> dates = new ArrayList<>();

        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }

        return dates;
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        if (days <= 0) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    private int normalizePage(Integer page) {
        if (page == null || page <= 0) {
            return 1;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return MATRIX_PAGE_SIZE;
        }
        return Math.min(pageSize, MATRIX_PAGE_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        String trimmed = keyword.trim();

        if (trimmed.length() > MAX_KEYWORD_LENGTH) {
            trimmed = trimmed.substring(0, MAX_KEYWORD_LENGTH);
        }

        return trimmed;
    }

    private String toDateString(Object value) {
        if (value instanceof Date) {
            return ((Date) value)
                    .toLocalDate()
                    .format(DATE_FORMAT);
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value)
                    .format(DATE_FORMAT);
        }
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(
                    ((Number) value).doubleValue()
            ).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(value))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}
