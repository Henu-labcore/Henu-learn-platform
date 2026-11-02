package com.hwz.personalized.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生画像：把学习记录、考核成绩、知识点掌握与学习计划聚合成
 * 「五维雷达 + 画像类型 + 标签 + 建议」。
 *
 * <p>设计约定（已与需求方确认）：
 * <ul>
 *   <li>执行维度在“没有学习计划”时隐藏（返回 null），雷达只画 4 轴；</li>
 *   <li>画像与画像历史只保留最近 30 天；</li>
 *   <li>学生端可见“类型 + 中性/正向标签”，负面判定只在教师端完整展示。</li>
 * </ul></p>
 */
@Service
public class StudentProfileService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int MAX_DAYS = 30;

    private static final double MAX_ENGAGEMENT_MINUTES = 600D;

    private static final double MAX_STABILITY_DAYS = 15D;

    private static final int MAX_TAGS = 5;

    private final JdbcTemplate jdbcTemplate;

    private final UserMapper userMapper;

    private final ObjectMapper objectMapper;

    public StudentProfileService(
            JdbcTemplate jdbcTemplate,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 计算并保存当日画像，返回给前端。
     */
    public Map<String, Object> getProfile(Long studentId, Integer days) {

        User student = userMapper.selectById(studentId);

        if (student == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "学生不存在"
            );
        }

        int resolvedDays = normalizeDays(days);

        LocalDate endDate = LocalDate.now();

        LocalDate startDate = endDate.minusDays(resolvedDays - 1L);

        // ---------- 知识维度 ----------
        List<Map<String, Object>> masteryRows =
                jdbcTemplate.queryForList(
                        "SELECT skm.knowledge_point_id,"
                                + " kp.name AS knowledge_point_name,"
                                + " kp.code AS knowledge_point_code,"
                                + " skm.mastery_score, skm.risk_level,"
                                + " skm.completed_learning_count,"
                                + " skm.total_learning_count,"
                                + " skm.graded_assignment_count,"
                                + " skm.total_assignment_count"
                                + " FROM student_knowledge_mastery skm"
                                + " JOIN knowledge_point kp"
                                + "   ON kp.knowledge_point_id = skm.knowledge_point_id"
                                + " WHERE skm.student_id = ?",
                        studentId
                );

        int contactedCount = 0;
        int highRiskCount = 0;
        int mediumRiskCount = 0;
        int lowRiskCount = 0;
        int unknownRiskCount = 0;
        double masterySum = 0D;

        List<Map<String, Object>> weakCandidates =
                new ArrayList<>();

        for (Map<String, Object> row : masteryRows) {
            String risk = String.valueOf(row.get("risk_level"));

            Map<String, Object> candidate =
                    new LinkedHashMap<>();
            candidate.put(
                    "knowledgePointId",
                    row.get("knowledge_point_id")
            );
            candidate.put(
                    "knowledgePointName",
                    row.get("knowledge_point_name")
            );
            candidate.put(
                    "knowledgePointCode",
                    row.get("knowledge_point_code")
            );
            candidate.put(
                    "masteryScore",
                    toDouble(row.get("mastery_score"))
            );
            candidate.put(
                    "riskLevel",
                    risk
            );
            candidate.put(
                    "completedLearningCount",
                    toInt(row.get("completed_learning_count"))
            );
            candidate.put(
                    "totalLearningCount",
                    toInt(row.get("total_learning_count"))
            );
            candidate.put(
                    "gradedAssignmentCount",
                    toInt(row.get("graded_assignment_count"))
            );
            candidate.put(
                    "totalAssignmentCount",
                    toInt(row.get("total_assignment_count"))
            );

            if ("UNKNOWN".equals(risk)) {
                unknownRiskCount++;
                continue;
            }

            contactedCount++;
            masterySum += toDouble(row.get("mastery_score"));

            if ("HIGH".equals(risk)) {
                highRiskCount++;
            } else if ("MEDIUM".equals(risk)) {
                mediumRiskCount++;
            } else if ("LOW".equals(risk)) {
                lowRiskCount++;
            }

            if ("HIGH".equals(risk) || "MEDIUM".equals(risk)) {
                weakCandidates.add(candidate);
            }
        }

        Double knowledgeScore = contactedCount == 0
                ? null
                : round1(masterySum / contactedCount);

        int totalActiveKnowledgePoints = toInt(
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM knowledge_point WHERE status = 'ACTIVE'",
                        Integer.class
                )
        );

        // ---------- 学习行为维度 ----------
        Map<String, Object> learning = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS record_count,"
                        + " COALESCE(SUM(learn_duration_sec), 0) AS duration_sec,"
                        + " COUNT(DISTINCT DATE(first_learn_time)) AS active_days,"
                        + " MAX(updated_at) AS last_active"
                        + " FROM learning_record"
                        + " WHERE user_id = ?"
                        + " AND first_learn_time >= ?",
                studentId,
                Date.valueOf(startDate)
        );

        int recordCount = toInt(learning.get("record_count"));
        long durationSec = toLong(learning.get("duration_sec"));
        int activeDays = toInt(learning.get("active_days"));
        long studyMinutes = durationSec / 60L;

        Double engagementScore = round1(
                clamp(studyMinutes / MAX_ENGAGEMENT_MINUTES * 100D)
        );

        Double stabilityScore = round1(
                clamp(activeDays / MAX_STABILITY_DAYS * 100D)
        );

        // ---------- 考核维度 ----------
        Map<String, Object> assessment = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS graded_count,"
                        + " AVG(s.score / a.total_score * 100) AS average_rate"
                        + " FROM assignment_submission s"
                        + " JOIN assignment a ON a.assignment_id = s.assignment_id"
                        + " WHERE s.student_id = ?"
                        + " AND s.status = 'GRADED'"
                        + " AND a.total_score > 0",
                studentId
        );

        int gradedCount = toInt(assessment.get("graded_count"));

        Double assessmentScore = assessment.get("average_rate") == null
                ? null
                : round1(clamp(toDouble(assessment.get("average_rate"))));

        // ---------- 执行维度（无计划则隐藏） ----------
        Map<String, Object> plan = jdbcTemplate.queryForMap(
                "SELECT COUNT(DISTINCT p.plan_id) AS plan_count,"
                        + " COUNT(pi.plan_item_id) AS item_count,"
                        + " COALESCE(SUM(lr.complete_time IS NOT NULL), 0) AS done_count,"
                        + " COALESCE(SUM(pi.target_date < CURDATE()"
                        + "   AND lr.complete_time IS NULL), 0) AS overdue_count"
                        + " FROM learning_plan p"
                        + " LEFT JOIN learning_plan_item pi ON pi.plan_id = p.plan_id"
                        + " LEFT JOIN learning_record lr"
                        + "   ON lr.user_id = p.user_id AND lr.item_pk = pi.item_pk"
                        + " WHERE p.user_id = ? AND p.status = 'ACTIVE'",
                studentId
        );

        int planCount = toInt(plan.get("plan_count"));
        int planItemCount = toInt(plan.get("item_count"));

        Double executionScore = null;

        if (planItemCount > 0) {
            int doneCount = toInt(plan.get("done_count"));
            int overdueCount = toInt(plan.get("overdue_count"));

            double completionRate = doneCount * 1.0 / planItemCount;
            double overdueRate = overdueCount * 1.0 / planItemCount;

            executionScore = round1(
                    clamp(completionRate * (1D - overdueRate) * 100D)
            );
        }

        // ---------- 拓展：收藏数（仅作标签依据） ----------
        int favoriteCount = toInt(
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM favorite WHERE user_id = ?",
                        Integer.class,
                        studentId
                )
        );

        // ---------- 掌握度趋势斜率（用于“稳步提升”判定） ----------
        double trendSlope = calculateTrendSlope(studentId);

        // ---------- 置信度 ----------
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

        // ---------- 标签 ----------
        List<Map<String, Object>> tags = buildTags(
                recordCount,
                activeDays,
                studyMinutes,
                contactedCount,
                totalActiveKnowledgePoints,
                highRiskCount,
                gradedCount,
                assessmentScore,
                executionScore,
                favoriteCount
        );

        // ---------- 画像类型 ----------
        Map<String, Object> persona = resolvePersona(
                recordCount,
                contactedCount,
                knowledgeScore,
                assessmentScore,
                engagementScore,
                stabilityScore,
                executionScore,
                highRiskCount,
                trendSlope
        );

        // ---------- 建议 ----------
        Map<String, Object> recommendation = buildRecommendation(
                studentId,
                weakCandidates
        );

        Map<String, Object> summary =
                new LinkedHashMap<>();
        summary.put(
                "averageMasteryScore",
                knowledgeScore
        );
        summary.put("highRiskCount", highRiskCount);
        summary.put("mediumRiskCount", mediumRiskCount);
        summary.put("lowRiskCount", lowRiskCount);
        summary.put("unknownRiskCount", unknownRiskCount);
        summary.put("contactedKnowledgePointCount", contactedCount);
        summary.put("totalKnowledgePointCount", totalActiveKnowledgePoints);
        summary.put("studyMinutes", studyMinutes);
        summary.put("activeDays", activeDays);
        summary.put("recordCount", recordCount);
        summary.put("gradedAssignmentCount", gradedCount);
        summary.put("planItemCount", planItemCount);
        summary.put("planCount", planCount);

        List<Map<String, Object>> dimensions =
                new ArrayList<>();

        dimensions.add(dimension(
                "knowledge",
                "知识掌握",
                knowledgeScore,
                contactedCount == 0
                        ? "尚未接触任何知识点"
                        : String.format(
                        "已接触 %d 个知识点，平均 %.1f，高风险 %d 个",
                        contactedCount,
                        knowledgeScore,
                        highRiskCount
                )
        ));

        dimensions.add(dimension(
                "assessment",
                "考核表现",
                assessmentScore,
                gradedCount == 0
                        ? "暂无已批改的考核"
                        : String.format(
                        "已批改 %d 次，平均得分率 %.1f",
                        gradedCount,
                        assessmentScore
                )
        ));

        dimensions.add(dimension(
                "engagement",
                "学习投入",
                engagementScore,
                String.format(
                        "近 %d 天学习 %d 分钟，%d 条学习记录",
                        resolvedDays,
                        studyMinutes,
                        recordCount
                )
        ));

        dimensions.add(dimension(
                "stability",
                "学习稳定",
                stabilityScore,
                String.format(
                        "近 %d 天活跃 %d 天",
                        resolvedDays,
                        activeDays
                )
        ));

        dimensions.add(dimension(
                "execution",
                "任务执行",
                executionScore,
                planItemCount == 0
                        ? "暂无学习计划"
                        : String.format(
                        "学习计划 %d 个，条目 %d 个",
                        planCount,
                        planItemCount
                )
        ));

        Map<String, Object> profile =
                new LinkedHashMap<>();
        profile.put(
                "student",
                studentView(student)
        );
        profile.put("days", resolvedDays);
        profile.put(
                "startDate",
                startDate.format(DATE_FORMAT)
        );
        profile.put(
                "endDate",
                endDate.format(DATE_FORMAT)
        );
        profile.put("dimensions", dimensions);
        profile.put("persona", persona);
        profile.put("tags", tags);
        profile.put(
                "confidence",
                confidenceView(
                        confidenceLevel,
                        confidenceLabel,
                        recordCount,
                        contactedCount
                )
        );
        profile.put("recommendation", recommendation);
        profile.put("summary", summary);
        profile.put(
                "updatedAt",
                LocalDateTime.now().toString()
        );

        saveSnapshot(studentId, profile);

        return profile;
    }

    /**
     * 画像历史：最近 N 天（最多 30 天）的维度分与画像类型。
     */
    public Map<String, Object> getProfileHistory(
            Long studentId,
            Integer days) {

        int resolvedDays = normalizeDays(days);

        LocalDate endDate = LocalDate.now();

        LocalDate startDate = endDate.minusDays(resolvedDays - 1L);

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT snapshot_date, knowledge_score,"
                                + " assessment_score, engagement_score,"
                                + " stability_score, execution_score,"
                                + " persona_code, persona_label"
                                + " FROM student_profile_snapshot"
                                + " WHERE student_id = ?"
                                + " AND snapshot_date >= ?"
                                + " ORDER BY snapshot_date",
                        studentId,
                        Date.valueOf(startDate)
                );

        Map<String, Map<String, Object>> rowsByDate =
                new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            rowsByDate.put(
                    toDateString(row.get("snapshot_date")),
                    row
            );
        }

        List<String> dates = new ArrayList<>();

        List<Map<String, Object>> series =
                new ArrayList<>();

        String[][] dimensionMeta = {
                {"knowledge", "知识掌握"},
                {"assessment", "考核表现"},
                {"engagement", "学习投入"},
                {"stability", "学习稳定"},
                {"execution", "任务执行"}
        };

        for (String[] meta : dimensionMeta) {
            Map<String, Object> dimension =
                    new LinkedHashMap<>();
            dimension.put("key", meta[0]);
            dimension.put("label", meta[1]);
            dimension.put(
                    "points",
                    new ArrayList<Map<String, Object>>()
            );
            series.add(dimension);
        }

        List<Map<String, Object>> personaTrend =
                new ArrayList<>();

        for (int index = 0; index < resolvedDays; index++) {
            LocalDate date = startDate.plusDays(index);
            String key = date.format(DATE_FORMAT);

            dates.add(key);

            Map<String, Object> row = rowsByDate.get(key);

            for (int i = 0; i < dimensionMeta.length; i++) {
                String column = dimensionMeta[i][0] + "_score";

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> points =
                        (List<Map<String, Object>>) series
                                .get(i)
                                .get("points");

                Map<String, Object> point =
                        new LinkedHashMap<>();
                point.put("date", key);
                point.put(
                        "score",
                        row == null
                                ? null
                                : toNullableDouble(row.get(column))
                );
                points.add(point);
            }

            Map<String, Object> personaPoint =
                    new LinkedHashMap<>();
            personaPoint.put("date", key);
            personaPoint.put(
                    "code",
                    row == null ? null : row.get("persona_code")
            );
            personaPoint.put(
                    "label",
                    row == null ? null : row.get("persona_label")
            );
            personaTrend.add(personaPoint);
        }

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
        response.put("dates", dates);
        response.put("series", series);
        response.put("persona", personaTrend);

        return response;
    }

    // ======================================================
    // 规则
    // ======================================================

    public List<Map<String, Object>> buildTags(
            int recordCount,
            int activeDays,
            long studyMinutes,
            int contactedCount,
            int totalKnowledgePoints,
            int highRiskCount,
            int gradedCount,
            Double assessmentScore,
            Double executionScore,
            int favoriteCount) {

        List<Map<String, Object>> tags =
                new ArrayList<>();

        if (recordCount < 5) {
            tags.add(tag(
                    "INSUFFICIENT",
                    "样本不足",
                    "画像生成中",
                    "ATTENTION",
                    String.format(
                            "近 30 天仅 %d 条学习记录，再积累一些数据画像会更准",
                            recordCount
                    )
            ));
        }

        if (activeDays == 0) {
            tags.add(tag(
                    "SILENT",
                    "近两周无学习",
                    "近期暂无学习",
                    "ATTENTION",
                    "近 30 天没有学习行为"
            ));
        } else if (activeDays >= 12) {
            tags.add(tag(
                    "PERSISTENT",
                    "持续学习",
                    "持续学习",
                    "POSITIVE",
                    String.format("近 30 天活跃 %d 天", activeDays)
            ));
        } else if (activeDays >= 3 && activeDays <= 7) {
            tags.add(tag(
                    "OCCASIONAL",
                    "偶发学习",
                    "间歇学习",
                    "NEUTRAL",
                    String.format("近 30 天活跃 %d 天", activeDays)
            ));
        }

        if (studyMinutes >= 600) {
            tags.add(tag(
                    "HIGH_ENGAGEMENT",
                    "高强度投入",
                    "投入充足",
                    "POSITIVE",
                    String.format("近 30 天学习 %d 分钟", studyMinutes)
            ));
        } else if (studyMinutes < 60 && recordCount > 0) {
            tags.add(tag(
                    "LOW_ENGAGEMENT",
                    "投入不足",
                    "投入可提升",
                    "ATTENTION",
                    String.format("近 30 天仅学习 %d 分钟", studyMinutes)
            ));
        }

        if (highRiskCount >= 3) {
            tags.add(tag(
                    "WEAK_MANY",
                    "高风险知识点较多",
                    "有可巩固的知识点",
                    "ATTENTION",
                    String.format("%d 个知识点处于高风险", highRiskCount)
            ));
        }

        if (totalKnowledgePoints > 0
                && contactedCount < totalKnowledgePoints * 0.5) {
            tags.add(tag(
                    "UNLEARNED_MANY",
                    "未学比例偏高",
                    "还有未接触的知识点",
                    "NEUTRAL",
                    String.format(
                            "已接触 %d/%d 个知识点",
                            contactedCount,
                            totalKnowledgePoints
                    )
            ));
        }

        if (gradedCount >= 2
                && assessmentScore != null
                && assessmentScore >= 80) {
            tags.add(tag(
                    "EXAM_STABLE",
                    "考核稳定",
                    "考核稳定",
                    "POSITIVE",
                    String.format(
                            "已批改 %d 次，平均 %.1f 分",
                            gradedCount,
                            assessmentScore
                    )
            ));
        }

        if (executionScore != null && executionScore >= 80) {
            tags.add(tag(
                    "PLAN_GOOD",
                    "计划执行良好",
                    "计划执行良好",
                    "POSITIVE",
                    String.format("计划执行得分 %.1f", executionScore)
            ));
        } else if (executionScore != null && executionScore < 50) {
            tags.add(tag(
                    "PLAN_POOR",
                    "执行待改善",
                    "计划执行可优化",
                    "ATTENTION",
                    String.format("计划执行得分 %.1f", executionScore)
            ));
        }

        if (favoriteCount >= 5) {
            tags.add(tag(
                    "COLLECTOR",
                    "收藏活跃",
                    "收藏活跃",
                    "POSITIVE",
                    String.format("已收藏 %d 张卡片", favoriteCount)
            ));
        }

        if (tags.size() > MAX_TAGS) {
            return new ArrayList<>(tags.subList(0, MAX_TAGS));
        }

        return tags;
    }

    public Map<String, Object> resolvePersona(
            int recordCount,
            int contactedCount,
            Double knowledgeScore,
            Double assessmentScore,
            Double engagementScore,
            Double stabilityScore,
            Double executionScore,
            int highRiskCount,
            double trendSlope) {

        if (recordCount < 5 || contactedCount < 5) {
            return persona(
                    "INSUFFICIENT",
                    "数据积累中",
                    "学习记录还不多，再完成几次学习和考核就能生成画像"
            );
        }

        if (stabilityScore != null
                && stabilityScore <= 0D) {
            return persona(
                    "SILENT",
                    "沉默型",
                    "近 30 天几乎没有学习行为，建议先恢复学习节奏"
            );
        }

        if (knowledgeScore != null
                && knowledgeScore >= 80
                && (assessmentScore == null
                || assessmentScore >= 80)
                && stabilityScore != null
                && stabilityScore >= 60) {
            return persona(
                    "EXCELLENT",
                    "全面优秀型",
                    "知识与考核表现都处于高位，保持节奏并拓展广度即可"
            );
        }

        List<Double> available = new ArrayList<>();

        if (knowledgeScore != null) {
            available.add(knowledgeScore);
        }
        if (assessmentScore != null) {
            available.add(assessmentScore);
        }
        if (engagementScore != null) {
            available.add(engagementScore);
        }
        if (stabilityScore != null) {
            available.add(stabilityScore);
        }
        if (executionScore != null) {
            available.add(executionScore);
        }

        if (available.size() >= 2) {
            double max = available.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0D);
            double min = available.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0D);

            if (max - min >= 30D) {
                return persona(
                        "IMBALANCED",
                        "偏科型",
                        "各维度差距较大，优先补齐明显偏低的维度"
                );
            }
        }

        if (knowledgeScore != null
                && knowledgeScore < 60
                && highRiskCount >= 3) {
            return persona(
                    "NEEDS_SUPPORT",
                    "薄弱待补型",
                    "多个知识点处于高风险，建议按补强清单逐个处理"
            );
        }

        if (executionScore != null
                && executionScore < 50) {
            return persona(
                    "EXECUTION",
                    "执行待改善型",
                    "计划完成度偏低，建议把大目标拆成更小的周任务"
            );
        }

        if (trendSlope > 0.1D
                && knowledgeScore != null
                && knowledgeScore >= 50
                && knowledgeScore <= 80) {
            return persona(
                    "IMPROVING",
                    "稳步提升型",
                    "掌握度在持续上升，保持当前节奏即可"
            );
        }

        return persona(
                "STEADY",
                "稳步积累型",
                "各维度较为均衡，按计划继续积累即可"
        );
    }

    private Map<String, Object> buildRecommendation(
            Long studentId,
            List<Map<String, Object>> weakCandidates) {

        weakCandidates.sort(
                Comparator
                        .comparingInt(
                                (Map<String, Object> item) ->
                                        riskOrder(
                                                String.valueOf(
                                                        item.get("riskLevel")
                                                )
                                        )
                        )
                        .thenComparingDouble(
                                item -> toDouble(
                                        item.get("masteryScore")
                                )
                        )
        );

        List<Map<String, Object>> weakPoints =
                new ArrayList<>();

        for (int index = 0;
                index < weakCandidates.size()
                        && index < 3;
                index++) {

            Map<String, Object> candidate =
                    weakCandidates.get(index);

            int completed = toInt(
                    candidate.get("completedLearningCount")
            );
            int total = toInt(
                    candidate.get("totalLearningCount")
            );
            int graded = toInt(
                    candidate.get("gradedAssignmentCount")
            );
            int totalAssignments = toInt(
                    candidate.get("totalAssignmentCount")
            );

            String action;

            if (total > completed) {
                action = String.format(
                        "完成剩余 %d 个学习资源",
                        total - completed
                );
            } else if (totalAssignments > 0 && graded == 0) {
                action = "提交对应能力考核";
            } else if (totalAssignments > 0) {
                action = "复习并重做对应考核";
            } else {
                action = "复习相关学习卡片";
            }

            Map<String, Object> point =
                    new LinkedHashMap<>();
            point.put(
                    "knowledgePointId",
                    candidate.get("knowledgePointId")
            );
            point.put(
                    "knowledgePointName",
                    candidate.get("knowledgePointName")
            );
            point.put(
                    "knowledgePointCode",
                    candidate.get("knowledgePointCode")
            );
            point.put(
                    "masteryScore",
                    candidate.get("masteryScore")
            );
            point.put(
                    "riskLevel",
                    candidate.get("riskLevel")
            );
            point.put("action", action);
            weakPoints.add(point);
        }

        // 继续项：最早开始但尚未完成的已发布卡片
        List<Map<String, Object>> continueRows =
                jdbcTemplate.queryForList(
                        "SELECT li.item_pk, li.title, lr.updated_at"
                                + " FROM learning_record lr"
                                + " JOIN learning_item li ON li.item_pk = lr.item_pk"
                                + " WHERE lr.user_id = ?"
                                + " AND lr.complete_time IS NULL"
                                + " AND lr.first_learn_time IS NOT NULL"
                                + " AND li.status = 'PUBLISHED'"
                                + " ORDER BY lr.updated_at ASC"
                                + " LIMIT 1",
                        studentId
                );

        Map<String, Object> continueItem = null;

        if (!continueRows.isEmpty()) {
            Map<String, Object> row = continueRows.get(0);
            continueItem = new LinkedHashMap<>();
            continueItem.put("itemPk", row.get("item_pk"));
            continueItem.put("title", row.get("title"));
            continueItem.put(
                    "lastActiveTime",
                    row.get("updated_at") == null
                            ? null
                            : String.valueOf(row.get("updated_at"))
            );
        }

        // 未学卡片（没有弱项、也没有继续项时）
        List<Map<String, Object>> unlearnedRows =
                jdbcTemplate.queryForList(
                        "SELECT li.item_pk, li.title"
                                + " FROM learning_item li"
                                + " LEFT JOIN learning_record lr"
                                + "   ON lr.item_pk = li.item_pk AND lr.user_id = ?"
                                + " WHERE li.status = 'PUBLISHED'"
                                + " AND lr.id IS NULL"
                                + " ORDER BY li.item_pk ASC"
                                + " LIMIT 1",
                        studentId
                );

        Map<String, Object> nextAction =
                new LinkedHashMap<>();

        if (!weakPoints.isEmpty()) {
            nextAction.put("type", "WEAK");
            nextAction.put(
                    "label",
                    "优先补强 " + weakPoints.size() + " 个知识点"
            );
        } else if (continueItem != null) {
            nextAction.put("type", "CONTINUE");
            nextAction.put(
                    "label",
                    "继续完成：" + continueItem.get("title")
            );
        } else if (!unlearnedRows.isEmpty()) {
            Map<String, Object> row = unlearnedRows.get(0);
            nextAction.put("type", "START");
            nextAction.put("itemPk", row.get("item_pk"));
            nextAction.put(
                    "label",
                    "开始学习：" + row.get("title")
            );
        } else {
            nextAction.put("type", "REVIEW");
            nextAction.put("label", "已完成全部学习项，建议复习巩固");
        }

        Map<String, Object> recommendation =
                new LinkedHashMap<>();
        recommendation.put("weakPoints", weakPoints);
        recommendation.put("continueItem", continueItem);
        recommendation.put("nextAction", nextAction);

        return recommendation;
    }

    // ======================================================
    // 持久化与工具
    // ======================================================

    private void saveSnapshot(
            Long studentId,
            Map<String, Object> profile) {

        LocalDate today = LocalDate.now();

        Double knowledge = dimensionScore(profile, "knowledge");
        Double assessment = dimensionScore(profile, "assessment");
        Double engagement = dimensionScore(profile, "engagement");
        Double stability = dimensionScore(profile, "stability");
        Double execution = dimensionScore(profile, "execution");

        Map<?, ?> persona = (Map<?, ?>) profile.get("persona");

        String tagsJson = "[]";

        try {
            tagsJson = objectMapper.writeValueAsString(
                    profile.get("tags")
            );
        } catch (Exception ignored) {
            // 标签序列化失败不影响画像本身
        }

        Map<?, ?> confidence =
                (Map<?, ?>) profile.get("confidence");

        jdbcTemplate.update(
                "INSERT INTO student_profile_snapshot ("
                        + " student_id, snapshot_date,"
                        + " knowledge_score, assessment_score,"
                        + " engagement_score, stability_score,"
                        + " execution_score, persona_code,"
                        + " persona_label, tags_json,"
                        + " confidence_level, calculated_at, created_at"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " ON DUPLICATE KEY UPDATE"
                        + " knowledge_score = VALUES(knowledge_score),"
                        + " assessment_score = VALUES(assessment_score),"
                        + " engagement_score = VALUES(engagement_score),"
                        + " stability_score = VALUES(stability_score),"
                        + " execution_score = VALUES(execution_score),"
                        + " persona_code = VALUES(persona_code),"
                        + " persona_label = VALUES(persona_label),"
                        + " tags_json = VALUES(tags_json),"
                        + " confidence_level = VALUES(confidence_level),"
                        + " calculated_at = VALUES(calculated_at)",
                studentId,
                Date.valueOf(today),
                knowledge,
                assessment,
                engagement,
                stability,
                execution,
                persona == null ? "INSUFFICIENT" : persona.get("code"),
                persona == null ? "数据积累中" : persona.get("label"),
                tagsJson,
                confidence == null ? "LOW" : confidence.get("level"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // 画像历史只保留最近 30 天
        jdbcTemplate.update(
                "DELETE FROM student_profile_snapshot"
                        + " WHERE student_id = ?"
                        + " AND snapshot_date < DATE_SUB(CURDATE(), INTERVAL 30 DAY)",
                studentId
        );
    }

    private Double dimensionScore(
            Map<String, Object> profile,
            String key) {

        Object dimensions = profile.get("dimensions");

        if (!(dimensions instanceof List)) {
            return null;
        }

        for (Object item : (List<?>) dimensions) {
            if (!(item instanceof Map)) {
                continue;
            }

            Map<?, ?> dimension = (Map<?, ?>) item;

            if (key.equals(dimension.get("key"))) {
                Object score = dimension.get("score");

                return score == null
                        ? null
                        : toDouble(score);
            }
        }

        return null;
    }

    private double calculateTrendSlope(Long studentId) {

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        "SELECT snapshot_date,"
                                + " ROUND(AVG(CASE WHEN risk_level <> 'UNKNOWN'"
                                + " THEN mastery_score END), 2) AS average_score"
                                + " FROM student_mastery_history"
                                + " WHERE student_id = ?"
                                + " AND snapshot_date >= ?"
                                + " GROUP BY snapshot_date"
                                + " ORDER BY snapshot_date",
                        studentId,
                        Date.valueOf(
                                LocalDate.now().minusDays(14)
                        )
                );

        if (rows.size() < 2) {
            return 0D;
        }

        Double first = toNullableDouble(
                rows.get(0).get("average_score")
        );
        Double last = toNullableDouble(
                rows.get(rows.size() - 1).get("average_score")
        );

        if (first == null || last == null) {
            return 0D;
        }

        Date firstDate = (Date) rows.get(0).get("snapshot_date");
        Date lastDate = (Date) rows.get(rows.size() - 1).get("snapshot_date");

        long days = lastDate.toLocalDate()
                .toEpochDay()
                - firstDate.toLocalDate()
                .toEpochDay();

        if (days <= 0) {
            return 0D;
        }

        return (last - first) / days;
    }

    private Map<String, Object> dimension(
            String key,
            String label,
            Double score,
            String evidence) {

        Map<String, Object> dimension =
                new LinkedHashMap<>();
        dimension.put("key", key);
        dimension.put("label", label);
        dimension.put("score", score);
        dimension.put("evidence", evidence);

        return dimension;
    }

    private Map<String, Object> tag(
            String code,
            String label,
            String studentLabel,
            String polarity,
            String evidence) {

        Map<String, Object> tag =
                new LinkedHashMap<>();
        tag.put("code", code);
        tag.put("label", label);
        tag.put("studentLabel", studentLabel);
        tag.put("polarity", polarity);
        tag.put("evidence", evidence);

        return tag;
    }

    private Map<String, Object> persona(
            String code,
            String label,
            String description) {

        Map<String, Object> persona =
                new LinkedHashMap<>();
        persona.put("code", code);
        persona.put("label", label);
        persona.put("description", description);

        return persona;
    }

    public Map<String, Object> confidenceView(
            String level,
            String label,
            int recordCount,
            int contactedCount) {

        Map<String, Object> confidence =
                new LinkedHashMap<>();
        confidence.put("level", level);
        confidence.put("label", label);
        confidence.put("recordCount", recordCount);
        confidence.put(
                "contactedKnowledgePointCount",
                contactedCount
        );

        return confidence;
    }

    private Map<String, Object> studentView(User student) {

        Map<String, Object> view =
                new LinkedHashMap<>();
        view.put("studentId", student.getId());
        view.put("username", student.getUsername());
        view.put(
                "displayName",
                student.getDisplayName() == null
                        ? student.getUsername()
                        : student.getDisplayName()
        );

        return view;
    }

    private String toDateString(Object value) {
        if (value instanceof Date) {
            return ((Date) value)
                    .toLocalDate()
                    .format(DATE_FORMAT);
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DATE_FORMAT);
        }
        return value == null ? null : String.valueOf(value);
    }

    private int riskOrder(String risk) {
        if ("HIGH".equals(risk)) {
            return 0;
        }
        if ("MEDIUM".equals(risk)) {
            return 1;
        }
        return 2;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return MAX_DAYS;
        }
        return Math.min(days, MAX_DAYS);
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
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
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
