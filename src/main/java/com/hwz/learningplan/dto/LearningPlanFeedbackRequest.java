package com.hwz.learningplan.dto;
import lombok.Data;

/** 完成心得独立保存，不覆盖原学习记录 complete_remark。 */
@Data
public class LearningPlanFeedbackRequest {
    private String reflection;
    private String difficulty;
    private String nextStep;
    private Integer rating;
    private Integer version;
}
