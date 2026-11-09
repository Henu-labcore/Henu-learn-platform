package com.hwz.learningplan.dto;
import lombok.Data;

/** 当前账号的截止提醒偏好，所有设备读取同一份设置。 */
@Data
public class LearningPlanReminderSetting {
    private Boolean enabled;
    private Integer dueDays;
}
