package com.example.computerassociation.controller;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateAnnouncementDTO;
import com.example.computerassociation.entity.Announcement;
import com.example.computerassociation.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理控制器
 * 提供公告的CRUD接口，实现权限控制
 */
@Tag(name = "公告管理", description = "宗门公告发布、查询、管理接口")
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Operation(summary = "获取已发布公告列表", description = "获取所有已发布的公告，根据用户权限过滤",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping
    @RequiresPermission("affair:view")
    public Result<List<Announcement>> getPublishedAnnouncements() {
        Long userId = UserContext.getUserId();
        List<Announcement> announcements = announcementService.getPublishedAnnouncements(userId);
        return Result.success(announcements);
    }

    @Operation(summary = "获取公告详情", description = "根据ID获取公告详情",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "公告不存在"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/{id}")
    @RequiresPermission("affair:view")
    public Result<Announcement> getAnnouncementById(@Parameter(description = "公告ID") @PathVariable Long id) {
        Announcement announcement = announcementService.getAnnouncementById(id);
        return Result.success(announcement);
    }

    @Operation(summary = "创建公告", description = "创建新公告，支持全局和本峰两种类型",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    @RequiresPermission("affair:announce_peak")
    public Result<Announcement> createAnnouncement(@RequestBody CreateAnnouncementDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        Announcement announcement = announcementService.createAnnouncement(userId, dto);
        return Result.success(announcement);
    }

    @Operation(summary = "更新公告", description = "更新公告内容",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}")
    @RequiresPermission("affair:manage_own_peak")
    public Result<Announcement> updateAnnouncement(
            @Parameter(description = "公告ID") @PathVariable Long id,
            @RequestBody CreateAnnouncementDTO dto) {
        Long userId = UserContext.getUserId();
        Announcement announcement = announcementService.updateAnnouncement(id, userId, dto);
        return Result.success(announcement);
    }

    @Operation(summary = "删除公告", description = "删除指定公告",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/{id}")
    @RequiresPermission("affair:manage_own_peak")
    public Result<Void> deleteAnnouncement(@Parameter(description = "公告ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        announcementService.deleteAnnouncement(id, userId);
        return Result.success(null);
    }

    @Operation(summary = "获取我的公告", description = "获取当前用户发布的所有公告",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/my")
    @RequiresPermission("affair:view")
    public Result<List<Announcement>> getMyAnnouncements() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        List<Announcement> announcements = announcementService.getMyAnnouncements(userId);
        return Result.success(announcements);
    }
}
