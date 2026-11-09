package com.hwz.contributor.controller;

import com.hwz.common.Result;
import com.hwz.contributor.dto.ContributorDtos;
import com.hwz.contributor.service.ContributorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 贡献者展示接口。所有已登录用户可见（登录校验由 AuthContextFilter 统一完成）。
 */
@RestController
@RequestMapping("/contributors")
public class ContributorController {

    private final ContributorService contributorService;

    public ContributorController(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    @GetMapping
    public Result<List<ContributorDtos.ContributorView>> list() {
        return Result.ok(contributorService.listPublic());
    }
}
