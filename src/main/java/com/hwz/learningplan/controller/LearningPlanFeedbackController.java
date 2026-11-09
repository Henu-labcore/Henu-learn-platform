package com.hwz.learningplan.controller;
import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.Result;
import com.hwz.learningplan.dto.LearningPlanFeedbackRequest;
import com.hwz.learningplan.service.LearningPlanFeedbackService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 以原 learning-plans 路径追加子资源，原计划接口不变。 */
@RestController @RequestMapping("/learning-plans/{planId}/feedback")
public class LearningPlanFeedbackController {
    private final AdminAccessService access;private final LearningPlanFeedbackService feedback;
    public LearningPlanFeedbackController(AdminAccessService access,LearningPlanFeedbackService feedback){this.access=access;this.feedback=feedback;}
    @GetMapping public Result<List<Map<String,Object>>> list(@PathVariable Long planId){return Result.ok(feedback.list(access.requireUser().getId(),planId));}
    @PutMapping("/{itemPk}") public Result<List<Map<String,Object>>> save(@PathVariable Long planId,@PathVariable Long itemPk,@RequestBody LearningPlanFeedbackRequest req){return Result.ok(feedback.save(access.requireUser().getId(),planId,itemPk,req));}
}
