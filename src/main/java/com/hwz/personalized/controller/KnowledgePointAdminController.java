package com.hwz.personalized.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.PageResponse;
import com.hwz.common.Result;
import com.hwz.common.entity.User;
import com.hwz.personalized.dto.KnowledgePointAdminDtos;
import com.hwz.personalized.service.KnowledgePointAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端知识点、前置关系和资源绑定接口。
 */
@RestController
@RequestMapping("/personalized/admin/knowledge-points")
public class KnowledgePointAdminController {

    private final KnowledgePointAdminService
            knowledgePointAdminService;

    private final AdminAccessService
            adminAccessService;

    public KnowledgePointAdminController(
            KnowledgePointAdminService
                    knowledgePointAdminService,
            AdminAccessService
                    adminAccessService) {

        this.knowledgePointAdminService =
                knowledgePointAdminService;

        this.adminAccessService =
                adminAccessService;
    }

    /**
     * 分页查询知识点。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points
     */
    @GetMapping
    public Result<PageResponse
            <KnowledgePointAdminDtos.KnowledgePointSummary>>
    list(
            @RequestParam(required = false)
                    String keyword,

            @RequestParam(required = false)
                    String status,

            @RequestParam(required = false)
                    String category,

            @RequestParam(defaultValue = "1")
                    long page,

            @RequestParam(defaultValue = "10")
                    long pageSize) {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .pageKnowledgePoints(
                                keyword,
                                status,
                                category,
                                normalizePage(page),
                                normalizePageSize(pageSize)
                        )
        );
    }

    /**
     * 查询知识点统计数据。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points/stats
     */
    @GetMapping("/stats")
    public Result
            <KnowledgePointAdminDtos.KnowledgePointStats>
    stats() {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .getStats()
        );
    }

    /**
     * 查询知识点完整详情。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points/1
     */
    @GetMapping("/{knowledgePointId}")
    public Result
            <KnowledgePointAdminDtos.KnowledgePointDetail>
    detail(
            @PathVariable
                    Long knowledgePointId) {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .getDetail(
                                knowledgePointId
                        )
        );
    }

    /**
     * 新增知识点并保存前置关系、学习资源和考核绑定。
     *
     * 请求示例：
     * POST /personalized/admin/knowledge-points
     */
    @PostMapping
    public Result
            <KnowledgePointAdminDtos.KnowledgePointDetail>
    create(
            @RequestBody
                    KnowledgePointAdminDtos
                            .KnowledgePointSaveRequest request) {

        User operator =
                adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService.create(
                        request,
                        operator
                )
        );
    }

    /**
     * 编辑知识点并整体替换全部关系。
     *
     * 请求示例：
     * PUT /personalized/admin/knowledge-points/1
     */
    @PutMapping("/{knowledgePointId}")
    public Result
            <KnowledgePointAdminDtos.KnowledgePointDetail>
    update(
            @PathVariable
                    Long knowledgePointId,

            @RequestBody
                    KnowledgePointAdminDtos
                            .KnowledgePointSaveRequest request) {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService.update(
                        knowledgePointId,
                        request
                )
        );
    }

    /**
     * 启用或停用知识点。
     *
     * 请求示例：
     * PUT /personalized/admin/knowledge-points/1/status
     */
    @PutMapping("/{knowledgePointId}/status")
    public Result
            <KnowledgePointAdminDtos.KnowledgePointDetail>
    updateStatus(
            @PathVariable
                    Long knowledgePointId,

            @RequestBody
                    KnowledgePointAdminDtos
                            .KnowledgePointStatusRequest request) {

        adminAccessService.requireAdmin();

        String status =
                request == null
                        ? null
                        : request.getStatus();

        return Result.ok(
                knowledgePointAdminService
                        .updateStatus(
                                knowledgePointId,
                                status
                        )
        );
    }

    /**
     * 查询可选前置知识点。
     *
     * 编辑知识点时，可通过 excludeKnowledgePointId
     * 排除当前知识点本身。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points/options/knowledge-points
     */
    @GetMapping("/options/knowledge-points")
    public Result<List
            <KnowledgePointAdminDtos.KnowledgePointOption>>
    knowledgePointOptions(
            @RequestParam(required = false)
                    Long excludeKnowledgePointId) {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .listKnowledgePointOptions(
                                excludeKnowledgePointId
                        )
        );
    }

    /**
     * 查询可绑定的学习资源。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points/options/learning-items
     */
    @GetMapping("/options/learning-items")
    public Result<List
            <KnowledgePointAdminDtos.LearningItemOption>>
    learningItemOptions() {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .listLearningItemOptions()
        );
    }

    /**
     * 查询可绑定的能力考核。
     *
     * 请求示例：
     * GET /personalized/admin/knowledge-points/options/assignments
     */
    @GetMapping("/options/assignments")
    public Result<List
            <KnowledgePointAdminDtos.AssignmentOption>>
    assignmentOptions() {

        adminAccessService.requireAdmin();

        return Result.ok(
                knowledgePointAdminService
                        .listAssignmentOptions()
        );
    }

    private long normalizePage(
            long page) {

        return page < 1
                ? 1
                : page;
    }

    private long normalizePageSize(
            long pageSize) {

        if (pageSize < 1) {
            return 10;
        }

        return Math.min(
                pageSize,
                100
        );
    }
}