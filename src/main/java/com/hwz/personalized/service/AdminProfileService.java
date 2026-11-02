package com.hwz.personalized.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端学生画像列表 / 概览 / 导出。
 *
 * <p>列表使用批量聚合（5 条分组 SQL）计算全部学生维度，
 * 再复用 {@link StudentProfileService} 的类型与标签规则，
 * 避免按学生逐个查询造成 N+1。</p>
 */
@Service
public class AdminProfileService {

    private static final int PAGE_SIZE = 8;

    private static final int MAX_PAGE_SIZE = 8;

    private static final double MAX_ENGAGEMENT_MINUTES = 600D;

    private static final double MAX_STABILITY_DAYS = 15D;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbcTemplate;

    private final StudentProfileService studentProfileService;

    public AdminProfileService(
            JdbcTemplate jdbcTemplate,
            StudentProfileService studentProfileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentProfileService = studentProfileService;
    }

    /**
     * 学生画像列表（分页 8 人、姓名/学号搜索、画像类型筛选）。
     */
    public Map<String, Object> getProfiles(
            String keyword,
            String persona,
            Integer page,
            Integer pageSize) {

        String searchKeyword = normalizeKeyword(keyword);

        String personaCode = persona == null
                ? ""
                : persona.trim().toUpperCase();

        int resolvedPageSize = normalizePageSize(pageSize);

        int resolvedPage = normalizePage(page);

        List<Map<String, Object>> all = computeAll();

        List<Map<String, Object>> filtered =
                new ArrayList<>();

        for (Map<String, Object> item : all) {
            if (!searchKeyword.isEmpty()
                    && !matchesKeyword(item, searchKeyword)) {
                continue;
            }

            if (!personaCode.isEmpty()) {
                Map<?, ?> currentPersona =
                        (Map<?, ?>) item.get("persona");

                if (currentPersona == null
                        || !personaCode.equals(
                        currentPersona.get("code"))) {
                    continue;
                }
            }

            filtered.add(item);
        }

        long total = filtered.size();

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil(total * 1.0 / resolvedPageSize);

        if (totalPages > 0 && resolvedPage > totalPages) {
            resolvedPage = totalPages;
        }

        int from = Math.min(
                (resolvedPage - 1) * resolvedPageSize,
                filtered.size()
        );

        int to = Math.min(
                from + resolvedPageSize,
                filtered.size()
        );

        List<Map<String, Object>> pageRows =
                new ArrayList<>(
                        filtered.subList(from, to)
                );

        Map<String, Object> response =
                new LinkedHashMap<>();
        response.put("page", resolvedPage);
        response.put("pageSize", resolvedPageSize);
        response.put("total", total);
        response.put("totalPages", totalPages);
        response.put("keyword", searchKeyword);
        response.put("persona", personaCode);
        response.put("students", pageRows);

        return response;
    }

    /**
     * 画像概览：类型分布、维度均值、置信度分布。
     */
    public Map<String, Object> getOverview() {

        List<Map<String, Object>> all = computeAll();

        Map<String, Integer> personaCounts =
                new LinkedHashMap<>();
        Map<String, Integer> confidenceCounts =
                new LinkedHashMap<>();
        Map<String, String> personaLabels =
                new LinkedHashMap<>();

        Map<String, double[]> dimensionSums =
                new LinkedHashMap<>();
        Map<String, Integer> dimensionCounts =
                new LinkedHashMap<>();

        Map<String, String> dimensionLabels =
                new LinkedHashMap<>();

        for (Map<String, Object> item : all) {
            Map<?, ?> persona =
                    (Map<?, ?>) item.get("persona");

            if (persona != null) {
                String code = String.valueOf(persona.get("code"));
                String label = String.valueOf(persona.get("label"));

                personaCounts.put(
                        code,
                        personaCounts.getOrDefault(code, 0) + 1
                );
                personaLabels.put(code, label);
            }

            Map<?, ?> confidence =
                    (Map<?, ?>) item.get("confidence");

            if (confidence != null) {
                String level = String.valueOf(confidence.get("level"));

                confidenceCounts.put(
                        level,
                        confidenceCounts.getOrDefault(level, 0) + 1
                );
            }

            List<?> dimensions =
                    (List<?>) item.get("dimensions");

            if (dimensions == null) {
                continue;
            }

            for (Object dimension : dimensions) {
                if (!(dimension instanceof Map)) {
                    continue;
                }

                Map<?, ?> map = (Map<?, ?>) dimension;

                String key = String.valueOf(map.get("key"));
                Object score = map.get("score");

                if (score == null) {
                    continue;
                }

                dimensionLabels.put(
                        key,
                        String.valueOf(map.get("label"))
                );
                dimensionSums
                        .computeIfAbsent(key, ignored -> new double[1])[0]
                        += toDouble(score);
                dimensionCounts.put(
                        key,
                        dimensionCounts.getOrDefault(key, 0) + 1
                );
            }
        }

        List<Map<String, Object>> personas =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : personaCounts.entrySet()) {
            Map<String, Object> view =
                    new LinkedHashMap<>();
            view.put("code", entry.getKey());
            view.put(
                    "label",
                    personaLabels.getOrDefault(
                            entry.getKey(),
                            entry.getKey()
                    )
            );
            view.put("count", entry.getValue());
            personas.add(view);
        }

        personas.sort((left, right) -> Integer.compare(
                (Integer) right.get("count"),
                (Integer) left.get("count")
        ));

        List<Map<String, Object>> dimensionViews =
                new ArrayList<>();

        for (Map.Entry<String, double[]> entry
                : dimensionSums.entrySet()) {

            int count = dimensionCounts.getOrDefault(
                    entry.getKey(),
                    0
            );

            Map<String, Object> view =
                    new LinkedHashMap<>();
            view.put("key", entry.getKey());
            view.put(
                    "label",
                    dimensionLabels.getOrDefault(
                            entry.getKey(),
                            entry.getKey()
                    )
            );
            view.put(
                    "average",
                    count == 0
                            ? null
                            : round1(entry.getValue()[0] / count)
            );
            view.put("coveredStudentCount", count);
            dimensionViews.add(view);
        }

        List<Map<String, Object>> confidenceViews =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : confidenceCounts.entrySet()) {
            Map<String, Object> view =
                    new LinkedHashMap<>();
            view.put("level", entry.getKey());
            view.put("count", entry.getValue());
            confidenceViews.add(view);
        }

        Map<String, Object> response =
                new LinkedHashMap<>();
        response.put("totalStudents", all.size());
        response.put("personas", personas);
        response.put("dimensions", dimensionViews);
        response.put("confidence", confidenceViews);
        response.put(
                "generatedDate",
                LocalDate.now().format(DATE_FORMAT)
        );

        return response;
    }

    /**
     * 单学生完整画像（教师端可见完整标签文案与证据）。
     */
    public Map<String, Object> getStudentProfile(Long studentId) {
        verifyStudent(studentId);

        return studentProfileService.getProfile(studentId, 30);
    }

    /**
     * 单学生画像历史（最近 30 天）。
     */
    public Map<String, Object> getStudentHistory(
            Long studentId,
            Integer days) {
        verifyStudent(studentId);

        return studentProfileService.getProfileHistory(
                studentId,
                days
        );
    }

    private void verifyStudent(Long studentId) {

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT role FROM sys_user WHERE user_id = ?",
                        studentId
                );

        if (rows.isEmpty()
                || !"USER".equals(
                String.valueOf(rows.get(0).get("role")))) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "学生不存在"
            );
        }
    }

    /**
     * 导出用：全部学生画像（不分页）。
     */
    public List<Map<String, Object>> getAllProfiles() {
        return computeAll();
    }

    /**
     * 导出用：按关键词与画像类型筛选后的全部学生画像。
     */
    public List<Map<String, Object>> getFilteredProfiles(
            String keyword,
            String persona) {

        String searchKeyword = normalizeKeyword(keyword);

        String personaCode = persona == null
                ? ""
                : persona.trim().toUpperCase();

        List<Map<String, Object>> filtered =
                new ArrayList<>();

        for (Map<String, Object> item : computeAll()) {
            if (!searchKeyword.isEmpty()
                    && !matchesKeyword(item, searchKeyword)) {
                continue;
            }

            if (!personaCode.isEmpty()) {
                Map<?, ?> currentPersona =
                        (Map<?, ?>) item.get("persona");

                if (currentPersona == null
                        || !personaCode.equals(
                        currentPersona.get("code"))) {
                    continue;
                }
            }

            filtered.add(item);
        }

        return filtered;
    }

    // ======================================================
    // 批量计算
    // ======================================================

    private List<Map<String, Object>> computeAll() {

        List<Map<String, Object>> students =
                jdbcTemplate.queryForList(
                        "SELECT user_id, username, display_name, real_name"
                                + " FROM sys_user"
                                + " WHERE role = 'USER' AND status = 'ACTIVE'"
                                + " ORDER BY user_id"
                );

        Map<Long, Map<String, Object>> knowledgeByStudent =
                indexBy(
                        jdbcTemplate.queryForList(
                                "SELECT student_id,"
                                        + " AVG(CASE WHEN risk_level <> 'UNKNOWN'"
                                        + "   THEN mastery_score END) AS average_score,"
                                        + " COALESCE(SUM(risk_level <> 'UNKNOWN'), 0) AS contacted_count,"
                                        + " COALESCE(SUM(risk_level = 'HIGH'), 0) AS high_count"
                                        + " FROM student_knowledge_mastery"
                                        + " GROUP BY student_id"
                        ),
                        "student_id"
                );

        Map<Long, Map<String, Object>> learningByStudent =
                indexBy(
                        jdbcTemplate.queryForList(
                                "SELECT user_id,"
                                        + " COUNT(*) AS record_count,"
                                        + " COALESCE(SUM(learn_duration_sec), 0) AS duration_sec,"
                                        + " COUNT(DISTINCT DATE(first_learn_time)) AS active_days"
                                        + " FROM learning_record"
                                        + " WHERE first_learn_time >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)"
                                        + " GROUP BY user_id"
                        ),
                        "user_id"
                );

        Map<Long, Map<String, Object>> assessmentByStudent =
                indexBy(
                        jdbcTemplate.queryForList(
                                "SELECT s.student_id,"
                                        + " COUNT(*) AS graded_count,"
                                        + " AVG(s.score / a.total_score * 100) AS average_rate"
                                        + " FROM assignment_submission s"
                                        + " JOIN assignment a"
                                        + "   ON a.assignment_id = s.assignment_id"
                                        + " WHERE s.status = 'GRADED' AND a.total_score > 0"
                                        + " GROUP BY s.student_id"
                        ),
                        "student_id"
                );

        Map<Long, Map<String, Object>> planByStudent =
                indexBy(
                        jdbcTemplate.queryForList(
                                "SELECT p.user_id,"
                                        + " COUNT(pi.plan_item_id) AS item_count,"
                                        + " COALESCE(SUM(lr.complete_time IS NOT NULL), 0) AS done_count,"
                                        + " COALESCE(SUM(pi.target_date < CURDATE()"
                                        + "   AND lr.complete_time IS NULL), 0) AS overdue_count"
                                        + " FROM learning_plan p"
                                        + " LEFT JOIN learning_plan_item pi"
                                        + "   ON pi.plan_id = p.plan_id"
                                        + " LEFT JOIN learning_record lr"
                                        + "   ON lr.user_id = p.user_id"
                                        + "   AND lr.item_pk = pi.item_pk"
                                        + " WHERE p.status = 'ACTIVE'"
                                        + " GROUP BY p.user_id"
                        ),
                        "user_id"
                );

        int totalKnowledgePoints = toInt(
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM knowledge_point WHERE status = 'ACTIVE'",
                        Integer.class
                )
        );

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Map<String, Object> student : students) {
            Long studentId = toLong(student.get("user_id"));

            Map<String, Object> knowledge = knowledgeByStudent.get(studentId);

            Double knowledgeScore = knowledge == null
                    ? null
                    : toNullableDouble(knowledge.get("average_score"));

            int contactedCount = knowledge == null
                    ? 0
                    : toInt(knowledge.get("contacted_count"));

            int highRiskCount = knowledge == null
                    ? 0
                    : toInt(knowledge.get("high_count"));

            if (knowledgeScore != null) {
                knowledgeScore = round1(knowledgeScore);
            }

            Map<String, Object> learning = learningByStudent.get(studentId);

            int recordCount = learning == null
                    ? 0
                    : toInt(learning.get("record_count"));

            long durationSec = learning == null
                    ? 0L
                    : toLong(learning.get("duration_sec"));

            int activeDays = learning == null
                    ? 0
                    : toInt(learning.get("active_days"));

            long studyMinutes = durationSec / 60L;

            Double engagementScore = round1(
                    clamp(studyMinutes / MAX_ENGAGEMENT_MINUTES * 100D)
            );

            Double stabilityScore = round1(
                    clamp(activeDays / MAX_STABILITY_DAYS * 100D)
            );

            Map<String, Object> assessment = assessmentByStudent.get(studentId);

            int gradedCount = assessment == null
                    ? 0
                    : toInt(assessment.get("graded_count"));

            Double assessmentScore = assessment == null
                    || assessment.get("average_rate") == null
                    ? null
                    : round1(clamp(
                    toDouble(assessment.get("average_rate"))
            ));

            Map<String, Object> plan = planByStudent.get(studentId);

            int planItemCount = plan == null
                    ? 0
                    : toInt(plan.get("item_count"));

            Double executionScore = null;

            if (planItemCount > 0) {
                int doneCount = toInt(plan.get("done_count"));
                int overdueCount = toInt(plan.get("overdue_count"));

                double completionRate = doneCount * 1.0 / planItemCount;
                double overdueRate = overdueCount * 1.0 / planItemCount;

                executionScore = round1(clamp(
                        completionRate * (1D - overdueRate) * 100D
                ));
            }

            String confidenceLevel;
            String confidenceLabel;

            if (recordCount >= 10 && contactedCount >= 5) {
                confidenceLevel = "HIGH";
                confidenceLabel = "高";
            } else if (recordCount >= 5 && contactedCount >= 3) {
                confidenceLevel = "MEDIUM";
                confidenceLabel = "中";
            } else {
                confidenceLevel = "LOW";
                confidenceLabel = "数据积累中";
            }

            List<Map<String, Object>> tags =
                    studentProfileService.buildTags(
                            recordCount,
                            activeDays,
                            studyMinutes,
                            contactedCount,
                            totalKnowledgePoints,
                            highRiskCount,
                            gradedCount,
                            assessmentScore,
                            executionScore,
                            0
                    );

            Map<String, Object> persona =
                    studentProfileService.resolvePersona(
                            recordCount,
                            contactedCount,
                            knowledgeScore,
                            assessmentScore,
                            engagementScore,
                            stabilityScore,
                            executionScore,
                            highRiskCount,
                            0D
                    );

            List<Map<String, Object>> dimensions =
                    new ArrayList<>();

            dimensions.add(dimension(
                    "knowledge",
                    "知识掌握",
                    knowledgeScore
            ));
            dimensions.add(dimension(
                    "assessment",
                    "考核表现",
                    assessmentScore
            ));
            dimensions.add(dimension(
                    "engagement",
                    "学习投入",
                    engagementScore
            ));
            dimensions.add(dimension(
                    "stability",
                    "学习稳定",
                    stabilityScore
            ));
            dimensions.add(dimension(
                    "execution",
                    "任务执行",
                    executionScore
            ));

            Map<String, Object> view =
                    new LinkedHashMap<>();
            view.put("studentId", studentId);
            view.put("username", student.get("username"));
            view.put(
                    "displayName",
                    student.get("display_name")
            );
            view.put("realName", student.get("real_name"));
            view.put("averageMasteryScore", knowledgeScore);
            view.put("highRiskCount", highRiskCount);
            view.put("contactedKnowledgePointCount", contactedCount);
            view.put("recordCount", recordCount);
            view.put("studyMinutes", studyMinutes);
            view.put("activeDays", activeDays);
            view.put("dimensions", dimensions);
            view.put("persona", persona);
            view.put(
                    "confidence",
                    studentProfileService.confidenceView(
                            confidenceLevel,
                            confidenceLabel,
                            recordCount,
                            contactedCount
                    )
            );
            view.put("tags", tags);

            result.add(view);
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        (Integer) item.get("highRiskCount")
                        )
                        .reversed()
                        .thenComparingDouble(
                                item -> {
                                    Double score =
                                            (Double) item.get(
                                                    "averageMasteryScore"
                                            );
                                    return score == null
                                            ? Double.MAX_VALUE
                                            : score;
                                }
                        )
                        .thenComparing(
                                item -> (Long) item.get("studentId")
                        )
        );

        return result;
    }

    private Map<Long, Map<String, Object>> indexBy(
            List<Map<String, Object>> rows,
            String keyColumn) {

        Map<Long, Map<String, Object>> indexed =
                new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long key = toLong(row.get(keyColumn));

            if (key != null) {
                indexed.put(key, row);
            }
        }

        return indexed;
    }

    private Map<String, Object> dimension(
            String key,
            String label,
            Double score) {

        Map<String, Object> dimension =
                new LinkedHashMap<>();
        dimension.put("key", key);
        dimension.put("label", label);
        dimension.put("score", score);

        return dimension;
    }

    private boolean matchesKeyword(
            Map<String, Object> item,
            String keyword) {

        String lower = keyword.toLowerCase();

        return String.valueOf(item.get("username"))
                .toLowerCase()
                .contains(lower)
                || String.valueOf(item.get("displayName"))
                .toLowerCase()
                .contains(lower)
                || String.valueOf(item.get("realName"))
                .toLowerCase()
                .contains(lower);
    }

    private int normalizePage(Integer page) {
        if (page == null || page <= 0) {
            return 1;
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }

        String trimmed = keyword.trim();

        return trimmed.length() > 50
                ? trimmed.substring(0, 50)
                : trimmed;
    }

    private double clamp(double value) {
        return Math.max(0D, Math.min(100D, value));
    }

    private double round1(double value) {
        return Math.round(value * 10D) / 10D;
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private Double toNullableDouble(Object value) {
        return value == null ? null : toDouble(value);
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

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
