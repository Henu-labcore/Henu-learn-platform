package com.hwz.user.controller;

import com.hwz.common.context.BaseContext;
import com.hwz.user.dto.AdminLearningSummaryResponse;
import com.hwz.user.dto.UserProgressDetailResponse;
import com.hwz.user.dto.LearningProgressTimeRequest;
import com.hwz.user.dto.LearningProgressTimeResponse;
import com.hwz.user.dto.LearningProgressResponse;
import com.hwz.user.dto.LearningProgressCompleteRequest;
import com.hwz.user.dto.LearningProgressCreateRequest;
import com.hwz.common.Result;
import com.hwz.user.service.LearningProgressService;
import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/learning-progress")
public class LearningProgressController {

    private final LearningProgressService learningProgressService;
    private final AdminAccessService accessService;

    public LearningProgressController(LearningProgressService learningProgressService, AdminAccessService accessService) {
        this.learningProgressService = learningProgressService;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<List<LearningProgressResponse>> getByUserAndItem(@RequestParam Long itemPk) {
        Long userId = requireUserId();
        if (itemPk == null || itemPk <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习项参数不正确");
        }
        return Result.ok(learningProgressService.listByUserAndItem(userId, itemPk));
    }

    @GetMapping("/user")
    public Result<List<LearningProgressResponse>> getByUser() {
        Long userId = requireUserId();
        return Result.ok(learningProgressService.listByUser(userId));
    }

    @GetMapping("/user/{userId}")
    public Result<List<LearningProgressResponse>> getByUserId(@PathVariable Long userId) {
        // allow admin or the user themselves
        User currentUser = accessService.requireUser();
        if (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该用户的学习进度");
        }
        return Result.ok(learningProgressService.listByUser(userId));
    }

    @GetMapping("/admin/summary")
    public Result<AdminLearningSummaryResponse> getAdminSummary() {
        accessService.requireAdmin();
        return Result.ok(learningProgressService.getAdminSummary());
    }

    @GetMapping("/admin/user/{userId}/detail")
    public Result<UserProgressDetailResponse> getUserProgressDetail(@PathVariable Long userId) {
        accessService.requireAdmin();
        return Result.ok(learningProgressService.getUserProgressDetail(userId));
    }

    @PostMapping("/enter")
    public Result<LearningProgressResponse> createFirstEntry(@RequestBody LearningProgressCreateRequest req) {
        Long userId = requireUserId();
        if (req == null || req.getItemPk() == null || req.getItemPk() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习项参数不正确");
        }
        return Result.ok(learningProgressService.createFirstEntry(userId, req.getItemPk()));
    }

    @PostMapping("/complete")
    public Result<LearningProgressResponse> complete(@RequestBody LearningProgressCompleteRequest req) {
        Long userId = requireUserId();
        if (req == null || req.getItemPk() == null || req.getItemPk() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习项参数不正确");
        }
        return Result.ok(learningProgressService.completeLearning(
                userId, req.getItemPk(), req.getCompleteRemark()));
    }

    @PostMapping("/time")
    public Result<LearningProgressTimeResponse> updateLearnDuration(@RequestBody LearningProgressTimeRequest req) {
        Long userId = requireUserId();
        if (req == null || req.getItemPk() == null || req.getItemPk() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习项参数不正确");
        }
        return Result.ok(learningProgressService.updateLearnDuration(
                userId, req.getItemPk(), req.getDeltaSec()));
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}
