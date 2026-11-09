package com.hwz.contributor.controller;

import com.hwz.common.Result;
import com.hwz.contributor.dto.ContributorDtos;
import com.hwz.contributor.service.ContributorService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 维护页接口：贡献者名单维护。全部操作仅限 ADMIN（Service 内统一调用 requireAdmin）。
 */
@RestController
@RequestMapping("/admin")
public class AdminMaintenanceController {

    private final ContributorService contributorService;

    public AdminMaintenanceController(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    /** 贡献者列表（含不可见记录） */
    @GetMapping("/contributors")
    public Result<List<ContributorDtos.ContributorAdminView>> list() {
        return Result.ok(contributorService.listForAdmin());
    }

    @PostMapping("/contributors")
    public Result<ContributorDtos.ContributorAdminView> create(
            @RequestBody ContributorDtos.ContributorSaveRequest request) {
        return Result.ok(contributorService.create(request));
    }

    @PutMapping("/contributors/{contributorId}")
    public Result<ContributorDtos.ContributorAdminView> update(
            @PathVariable Long contributorId,
            @RequestBody ContributorDtos.ContributorSaveRequest request) {
        return Result.ok(contributorService.update(contributorId, request));
    }

    @DeleteMapping("/contributors/{contributorId}")
    public Result<Void> delete(@PathVariable Long contributorId) {
        contributorService.delete(contributorId);
        return Result.ok();
    }
}
