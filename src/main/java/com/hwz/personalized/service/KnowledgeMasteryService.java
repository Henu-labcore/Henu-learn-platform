package com.hwz.personalized.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hwz.common.entity.KnowledgePoint;
import com.hwz.common.entity.StudentKnowledgeMastery;
import com.hwz.common.entity.User;
import com.hwz.common.mapper.KnowledgePointMapper;
import com.hwz.common.mapper.StudentKnowledgeMasteryMapper;
import com.hwz.common.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * 学生知识点掌握度计算服务。
 *
 * 主要数据来源：
 * 1. 学习资源完成情况；
 * 2. 已批改考核成绩；
 * 3. 知识点与资源、考核的权重关系。
 */
@Service
public class KnowledgeMasteryService {

    /**
     * 已开始但未完成学习资源时，
     * 给予 40 分的过程性学习得分。
     */
    private static final BigDecimal STARTED_SCORE =
            new BigDecimal("40.00");

    /**
     * 学习行为在综合掌握度中的权重。
     */
    private static final BigDecimal LEARNING_RATIO =
            new BigDecimal("0.40");

    /**
     * 考核成绩在综合掌握度中的权重。
     */
    private static final BigDecimal ASSESSMENT_RATIO =
            new BigDecimal("0.60");

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100.00");

    private static final BigDecimal MEDIUM_RANGE =
            new BigDecimal("15.00");

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final StudentKnowledgeMasteryMapper masteryMapper;

    public KnowledgeMasteryService(
            JdbcTemplate jdbcTemplate,
            UserMapper userMapper,
            KnowledgePointMapper knowledgePointMapper,
            StudentKnowledgeMasteryMapper masteryMapper) {
        this.jdbcTemplate =
                jdbcTemplate;

        this.userMapper =
                userMapper;

        this.knowledgePointMapper =
                knowledgePointMapper;

        this.masteryMapper =
                masteryMapper;
    }

    /**
     * 重新计算指定学生的全部启用知识点掌握度。
     *
     * 每个知识点最终只保留一条掌握度记录。
     */
    @Transactional
    public List<StudentKnowledgeMastery> recalculateStudent(
            Long studentId) {
        User student =
                validateStudent(studentId);

        List<KnowledgePoint> knowledgePoints =
                knowledgePointMapper.selectList(
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

        List<StudentKnowledgeMastery> results =
                new ArrayList<>();

        for (KnowledgePoint knowledgePoint
                : knowledgePoints) {
            StudentKnowledgeMastery mastery =
                    calculateKnowledgePoint(
                            student.getId(),
                            knowledgePoint
                    );

            saveOrUpdate(mastery);

            results.add(mastery);
        }

        return results;
    }

    /**
     * 重新计算全部启用学生的掌握度。
     *
     * 管理员"全部重算"使用：
     * 新建知识点或调整绑定关系后，
     * 一键让所有学生的最新掌握度保持一致。
     */
    @Transactional
    public Map<String, Object> recalculateAllStudents() {
        List<User> students =
                userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .eq(
                                        User::getRole,
                                        User.Role.USER
                                )
                                .eq(
                                        User::getStatus,
                                        User.Status.ACTIVE
                                )
                );

        int studentCount = 0;
        int recordCount = 0;

        for (User student : students) {
            List<StudentKnowledgeMastery> results =
                    recalculateStudent(
                            student.getId()
                    );

            studentCount++;

            recordCount += results.size();
        }

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "studentCount",
                studentCount
        );

        result.put(
                "recordCount",
                recordCount
        );

        return result;
    }

    /**
     * 计算一个学生在单个知识点上的掌握度。
     */
    private StudentKnowledgeMastery calculateKnowledgePoint(
            Long studentId,
            KnowledgePoint knowledgePoint) {
        LearningEvidence learningEvidence =
                queryLearningEvidence(
                        studentId,
                        knowledgePoint.getKnowledgePointId()
                );

        AssessmentEvidence assessmentEvidence =
                queryAssessmentEvidence(
                        studentId,
                        knowledgePoint.getKnowledgePointId()
                );

        BigDecimal learningScore =
                calculateWeightedScore(
                        learningEvidence.weightedScore,
                        learningEvidence.totalWeight
                );

        BigDecimal assessmentScore =
                calculateWeightedScore(
                        assessmentEvidence.weightedScore,
                        assessmentEvidence.totalWeight
                );

        boolean hasLearningRelation =
                learningEvidence.totalWeight.compareTo(
                        ZERO
                ) > 0;

        boolean hasAssessmentRelation =
                assessmentEvidence.totalWeight.compareTo(
                        ZERO
                ) > 0;

        BigDecimal masteryScore =
                calculateMasteryScore(
                        learningScore,
                        assessmentScore,
                        hasLearningRelation,
                        hasAssessmentRelation
                );

        BigDecimal warningThreshold =
                knowledgePoint.getWarningThreshold();

        if (warningThreshold == null) {
            warningThreshold =
                    new BigDecimal("60.00");
        }

        /*
         * 判断学生是否接触过该知识点：
         * 只要关联资源有过学习记录（含开始未完成）
         * 或关联考核有过提交记录（含未批改），
         * 就视为"已接触"；两者皆无视为"未学习"。
         */
        boolean hasContact =
                learningEvidence.lastLearningTime != null
                || assessmentEvidence.lastAssessmentTime != null;

        String riskLevel =
                calculateRiskLevel(
                        masteryScore,
                        warningThreshold,
                        hasLearningRelation,
                        hasAssessmentRelation,
                        hasContact
                );

        LocalDateTime now =
                LocalDateTime.now();

        StudentKnowledgeMastery mastery =
                new StudentKnowledgeMastery();

        mastery.setStudentId(studentId);

        mastery.setKnowledgePointId(
                knowledgePoint.getKnowledgePointId()
        );

        mastery.setLearningScore(
                learningScore
        );

        mastery.setAssessmentScore(
                assessmentScore
        );

        mastery.setMasteryScore(
                masteryScore
        );

        mastery.setRiskLevel(
                riskLevel
        );

        mastery.setCompletedLearningCount(
                learningEvidence.completedCount
        );

        mastery.setTotalLearningCount(
                learningEvidence.totalCount
        );

        mastery.setGradedAssignmentCount(
                assessmentEvidence.gradedCount
        );

        mastery.setTotalAssignmentCount(
                assessmentEvidence.totalCount
        );

        mastery.setLastLearningTime(
                learningEvidence.lastLearningTime
        );

        mastery.setLastAssessmentTime(
                assessmentEvidence.lastAssessmentTime
        );

        mastery.setCalculatedAt(now);

        mastery.setUpdatedAt(now);

        return mastery;
    }

    /**
     * 查询一个知识点对应的学习行为证据。
     *
     * 计分规则：
     * 已完成：100 分；
     * 已开始未完成：40 分；
     * 未开始：0 分。
     */
    private LearningEvidence queryLearningEvidence(
            Long studentId,
            Long knowledgePointId) {
        String sql =
                "SELECT "
                        + "COALESCE(SUM(likp.weight), 0) "
                        + "    AS total_weight, "

                        + "COALESCE(SUM( "
                        + "    likp.weight * "
                        + "    CASE "
                        + "        WHEN lr.complete_time IS NOT NULL "
                        + "            THEN 100.00 "
                        + "        WHEN lr.first_learn_time IS NOT NULL "
                        + "            THEN ? "
                        + "        ELSE 0.00 "
                        + "    END "
                        + "), 0) AS weighted_score, "

                        + "COUNT(DISTINCT likp.item_pk) "
                        + "    AS total_count, "

                        + "COUNT(DISTINCT "
                        + "    CASE "
                        + "        WHEN lr.complete_time IS NOT NULL "
                        + "        THEN likp.item_pk "
                        + "    END "
                        + ") AS completed_count, "

                        + "MAX( "
                        + "    COALESCE( "
                        + "        lr.complete_time, "
                        + "        lr.first_learn_time "
                        + "    ) "
                        + ") AS last_learning_time "

                        + "FROM learning_item_knowledge_point likp "

                        + "INNER JOIN learning_item li "
                        + "    ON li.item_pk = likp.item_pk "
                        + "   AND li.status = 'PUBLISHED' "

                        + "LEFT JOIN learning_record lr "
                        + "    ON lr.item_pk = likp.item_pk "
                        + "   AND lr.user_id = ? "

                        + "WHERE likp.knowledge_point_id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                (resultSet, rowNumber) -> {
                    LearningEvidence evidence =
                            new LearningEvidence();

                    evidence.totalWeight =
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "total_weight"
                                    )
                            );

                    evidence.weightedScore =
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "weighted_score"
                                    )
                            );

                    evidence.totalCount =
                            resultSet.getInt(
                                    "total_count"
                            );

                    evidence.completedCount =
                            resultSet.getInt(
                                    "completed_count"
                            );

                    evidence.lastLearningTime =
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "last_learning_time"
                                    )
                            );

                    return evidence;
                },
                STARTED_SCORE,
                studentId,
                knowledgePointId
        );
    }

    /**
     * 查询一个知识点对应的考核证据。
     *
     * 只有状态为 GRADED 且 score 不为空的记录
     * 才计入考核得分。
     *
     * 未批改、退回、逾期等情况当前按 0 分处理，
     * 但仍计入关联考核总数。
     */
    private AssessmentEvidence queryAssessmentEvidence(
            Long studentId,
            Long knowledgePointId) {
        String sql =
                "SELECT "
                        + "COALESCE(SUM(akp.weight), 0) "
                        + "    AS total_weight, "

                        + "COALESCE(SUM( "
                        + "    akp.weight * "
                        + "    CASE "
                        + "        WHEN s.status = 'GRADED' "
                        + "         AND s.score IS NOT NULL "
                        + "         AND a.total_score > 0 "
                        + "        THEN LEAST( "
                        + "            100.00, "
                        + "            GREATEST( "
                        + "                0.00, "
                        + "                s.score "
                        + "                / NULLIF(a.total_score, 0) "
                        + "                * 100.00 "
                        + "            ) "
                        + "        ) "
                        + "        ELSE 0.00 "
                        + "    END "
                        + "), 0) AS weighted_score, "

                        + "COUNT(DISTINCT akp.assignment_id) "
                        + "    AS total_count, "

                        + "COUNT(DISTINCT "
                        + "    CASE "
                        + "        WHEN s.status = 'GRADED' "
                        + "         AND s.score IS NOT NULL "
                        + "        THEN akp.assignment_id "
                        + "    END "
                        + ") AS graded_count, "

                        + "MAX( "
                        + "    COALESCE( "
                        + "        s.graded_at, "
                        + "        s.submitted_at "
                        + "    ) "
                        + ") AS last_assessment_time "

                        + "FROM assignment_knowledge_point akp "

                        + "INNER JOIN assignment a "
                        + "    ON a.assignment_id = "
                        + "       akp.assignment_id "
                        + "   AND a.status = 'PUBLISHED' "

                        + "LEFT JOIN assignment_submission s "
                        + "    ON s.assignment_id = "
                        + "       akp.assignment_id "
                        + "   AND s.student_id = ? "

                        + "WHERE akp.knowledge_point_id = ?";

        return jdbcTemplate.queryForObject(
                sql,
                (resultSet, rowNumber) -> {
                    AssessmentEvidence evidence =
                            new AssessmentEvidence();

                    evidence.totalWeight =
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "total_weight"
                                    )
                            );

                    evidence.weightedScore =
                            safeDecimal(
                                    resultSet.getBigDecimal(
                                            "weighted_score"
                                    )
                            );

                    evidence.totalCount =
                            resultSet.getInt(
                                    "total_count"
                            );

                    evidence.gradedCount =
                            resultSet.getInt(
                                    "graded_count"
                            );

                    evidence.lastAssessmentTime =
                            toLocalDateTime(
                                    resultSet.getTimestamp(
                                            "last_assessment_time"
                                    )
                            );

                    return evidence;
                },
                studentId,
                knowledgePointId
        );
    }

    /**
     * 使用关联权重计算百分制得分。
     */
    private BigDecimal calculateWeightedScore(
            BigDecimal weightedScore,
            BigDecimal totalWeight) {
        if (totalWeight == null
                || totalWeight.compareTo(ZERO) <= 0) {
            return ZERO;
        }

        BigDecimal result =
                safeDecimal(weightedScore)
                        .divide(
                                totalWeight,
                                2,
                                RoundingMode.HALF_UP
                        );

        return limitScore(result);
    }

    /**
     * 计算综合掌握度。
     */
    private BigDecimal calculateMasteryScore(
            BigDecimal learningScore,
            BigDecimal assessmentScore,
            boolean hasLearningRelation,
            boolean hasAssessmentRelation) {
        if (hasLearningRelation
                && hasAssessmentRelation) {
            BigDecimal result =
                    learningScore.multiply(
                            LEARNING_RATIO
                    ).add(
                            assessmentScore.multiply(
                                    ASSESSMENT_RATIO
                            )
                    );

            return limitScore(
                    result.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );
        }

        if (hasLearningRelation) {
            return limitScore(
                    learningScore
            );
        }

        if (hasAssessmentRelation) {
            return limitScore(
                    assessmentScore
            );
        }

        return ZERO;
    }

    /**
     * 根据掌握度和知识点阈值计算风险等级。
     *
     * 两种情况判为 UNKNOWN：
     * 1. 知识点没有绑定任何学习资源与考核（无关联）；
     * 2. 学生从未接触过该知识点（无学习记录且无考核提交）。
     *
     * 其余按掌握度与阈值的关系判 HIGH / MEDIUM / LOW。
     */
    private String calculateRiskLevel(
            BigDecimal masteryScore,
            BigDecimal warningThreshold,
            boolean hasLearningRelation,
            boolean hasAssessmentRelation,
            boolean hasContact) {
        if ((!hasLearningRelation
                && !hasAssessmentRelation)
                || !hasContact) {
            return "UNKNOWN";
        }

        if (masteryScore.compareTo(
                warningThreshold
        ) < 0) {
            return "HIGH";
        }

        BigDecimal mediumUpperLimit =
                warningThreshold.add(
                        MEDIUM_RANGE
                );

        if (masteryScore.compareTo(
                mediumUpperLimit
        ) < 0) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**
     * 新增或更新学生与知识点唯一掌握度记录。
     */
    private void saveOrUpdate(
            StudentKnowledgeMastery newMastery) {
        StudentKnowledgeMastery existing =
                masteryMapper.selectOne(
                        new LambdaQueryWrapper
                                <StudentKnowledgeMastery>()
                                .eq(
                                        StudentKnowledgeMastery
                                                ::getStudentId,
                                        newMastery.getStudentId()
                                )
                                .eq(
                                        StudentKnowledgeMastery
                                                ::getKnowledgePointId,
                                        newMastery
                                                .getKnowledgePointId()
                                )
                                .last("LIMIT 1")
                );

        if (existing == null) {
            newMastery.setCreatedAt(
                    LocalDateTime.now()
            );

            masteryMapper.insert(
                    newMastery
            );

            return;
        }

        existing.setLearningScore(
                newMastery.getLearningScore()
        );

        existing.setAssessmentScore(
                newMastery.getAssessmentScore()
        );

        existing.setMasteryScore(
                newMastery.getMasteryScore()
        );

        existing.setRiskLevel(
                newMastery.getRiskLevel()
        );

        existing.setCompletedLearningCount(
                newMastery
                        .getCompletedLearningCount()
        );

        existing.setTotalLearningCount(
                newMastery
                        .getTotalLearningCount()
        );

        existing.setGradedAssignmentCount(
                newMastery
                        .getGradedAssignmentCount()
        );

        existing.setTotalAssignmentCount(
                newMastery
                        .getTotalAssignmentCount()
        );

        existing.setLastLearningTime(
                newMastery.getLastLearningTime()
        );

        existing.setLastAssessmentTime(
                newMastery
                        .getLastAssessmentTime()
        );

        existing.setCalculatedAt(
                newMastery.getCalculatedAt()
        );

        existing.setUpdatedAt(
                newMastery.getUpdatedAt()
        );

        masteryMapper.updateById(
                existing
        );

        /*
         * 将数据库中的原主键回填到返回对象，
         * 便于控制器后续返回完整结果。
         */
        newMastery.setMasteryId(
                existing.getMasteryId()
        );

        newMastery.setCreatedAt(
                existing.getCreatedAt()
        );
    }

    /**
     * 验证目标用户是正常启用的普通用户。
     */
    private User validateStudent(
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
                    "Only ordinary users can calculate mastery"
            );
        }

        if (student.getStatus()
                != User.Status.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only active students can calculate mastery"
            );
        }

        return student;
    }

    /**
     * 将得分限制在 0 到 100。
     */
    private BigDecimal limitScore(
            BigDecimal score) {
        BigDecimal safeScore =
                safeDecimal(score);

        if (safeScore.compareTo(ZERO) < 0) {
            return ZERO;
        }

        if (safeScore.compareTo(
                ONE_HUNDRED
        ) > 0) {
            return ONE_HUNDRED;
        }

        return safeScore.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal safeDecimal(
            BigDecimal value) {
        if (value == null) {
            return ZERO;
        }

        return value;
    }

    private LocalDateTime toLocalDateTime(
            Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }

    /**
     * 学习行为查询结果。
     */
    private static class LearningEvidence {

        private BigDecimal totalWeight =
                ZERO;

        private BigDecimal weightedScore =
                ZERO;

        private int totalCount;

        private int completedCount;

        private LocalDateTime lastLearningTime;
    }

    /**
     * 考核表现查询结果。
     */
    private static class AssessmentEvidence {

        private BigDecimal totalWeight =
                ZERO;

        private BigDecimal weightedScore =
                ZERO;

        private int totalCount;

        private int gradedCount;

        private LocalDateTime lastAssessmentTime;
    }
}