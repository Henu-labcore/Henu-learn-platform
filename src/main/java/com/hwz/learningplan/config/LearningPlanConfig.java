package com.hwz.learningplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

/** 学习计划专用时钟；不替换原平台配置，测试可注入固定时间。 */
@Configuration
public class LearningPlanConfig {
    @Bean("learningPlanClock")
    public Clock learningPlanClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
