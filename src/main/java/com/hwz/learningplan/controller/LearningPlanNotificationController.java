package com.hwz.learningplan.controller;
import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.Result;
import com.hwz.learningplan.dto.LearningPlanReminderSetting;
import com.hwz.learningplan.service.LearningPlanNotificationService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 通知与偏好追加到原学习计划资源，不新增通用平台通知系统。 */
@RestController @RequestMapping("/learning-plans")
public class LearningPlanNotificationController {
    private final AdminAccessService access;private final LearningPlanNotificationService service;
    public LearningPlanNotificationController(AdminAccessService access,LearningPlanNotificationService service){this.access=access;this.service=service;}
    private Long user(){return access.requireUser().getId();}
    @GetMapping("/notifications") public Result<List<Map<String,Object>>> list(){return Result.ok(service.list(user()));}
    @PostMapping("/notifications/{id}/read") public Result<Void> read(@PathVariable Long id){service.read(user(),id);return Result.ok(null);}
    @PostMapping("/notifications/read-all") public Result<Void> readAll(){service.readAll(user());return Result.ok(null);}
    @GetMapping("/reminder-setting") public Result<LearningPlanReminderSetting> setting(){return Result.ok(service.setting(user()));}
    @PutMapping("/reminder-setting") public Result<LearningPlanReminderSetting> save(@RequestBody LearningPlanReminderSetting req){return Result.ok(service.saveSetting(user(),req));}
}
