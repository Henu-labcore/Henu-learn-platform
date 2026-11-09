package com.hwz.learningplan.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.PageResponse;
import com.hwz.common.Result;
import com.hwz.common.context.BaseContext;
import com.hwz.learningplan.dto.LearningPlanDtos.AdminRiskResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.DetailResponse;
import com.hwz.learningplan.dto.LearningPlanDtos.SaveRequest;
import com.hwz.learningplan.service.LearningPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class LearningPlanController {
    private final LearningPlanService service;
    private final AdminAccessService accessService;

    public LearningPlanController(LearningPlanService service, AdminAccessService accessService) {
        this.service = service;
        this.accessService = accessService;
    }

    @GetMapping("/learning-plans/current")
    public Result<DetailResponse> current() {
        return Result.ok(service.getCurrent(requireUserId()));
    }

    @GetMapping("/learning-plans/history")
    public Result<List<DetailResponse>> history() {
        return Result.ok(service.getHistory(requireUserId()));
    }

    @GetMapping("/learning-plans/{planId}")
    public Result<DetailResponse> detail(@PathVariable Long planId) {
        return Result.ok(service.getById(requireUserId(), planId));
    }

    @PostMapping("/learning-plans")
    public Result<DetailResponse> create(@RequestBody SaveRequest request) {
        return Result.ok(service.create(requireUserId(), request));
    }

    @PutMapping("/learning-plans/{planId}")
    public Result<DetailResponse> update(@PathVariable Long planId, @RequestBody SaveRequest request) {
        return Result.ok(service.update(requireUserId(), planId, request));
    }

    @PostMapping("/learning-plans/{planId}/cancel")
    public Result<DetailResponse> cancel(@PathVariable Long planId) {
        return Result.ok(service.cancel(requireUserId(), planId));
    }

    @GetMapping("/admin/learning-plan-risks")
    public Result<PageResponse<AdminRiskResponse>> adminRisks(
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        accessService.requireAdmin();
        return Result.ok(service.getAdminRisks(riskLevel, keyword, page, pageSize));
    }

    private Long requireUserId() {
        // 复用原平台账号有效性检查；客户端不能指定其他用户的身份。
        return accessService.requireUser().getId();
    }
}
//学生端接口 + 管理员风险查询
