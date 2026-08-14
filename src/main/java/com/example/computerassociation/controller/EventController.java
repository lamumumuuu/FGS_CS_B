package com.example.computerassociation.controller;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateEventDTO;
import com.example.computerassociation.entity.Event;
import com.example.computerassociation.service.EventService;
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
 * 活动管理控制器
 * 提供活动的CRUD接口，实现权限控制
 */
@Tag(name = "活动管理", description = "宗门活动创建、查询、管理接口")
@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Operation(summary = "获取活动列表", description = "获取进行中和已规划的活动列表",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping
    @RequiresPermission("affair:view")
    public Result<List<Event>> getActiveEvents() {
        Long userId = UserContext.getUserId();
        List<Event> events = eventService.getActiveEvents(userId);
        return Result.success(events);
    }

    @Operation(summary = "获取活动详情", description = "根据ID获取活动详情",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "活动不存在"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/{id}")
    @RequiresPermission("affair:view")
    public Result<Event> getEventById(@Parameter(description = "活动ID") @PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return Result.success(event);
    }

    @Operation(summary = "创建活动", description = "创建新的宗门活动",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    @RequiresPermission("affair:create_event")
    public Result<Event> createEvent(@RequestBody CreateEventDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        Event event = eventService.createEvent(userId, dto);
        return Result.success(event);
    }

    @Operation(summary = "更新活动", description = "更新活动信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}")
    @RequiresPermission("affair:manage_own_peak")
    public Result<Event> updateEvent(
            @Parameter(description = "活动ID") @PathVariable Long id,
            @RequestBody CreateEventDTO dto) {
        Long userId = UserContext.getUserId();
        Event event = eventService.updateEvent(id, userId, dto);
        return Result.success(event);
    }

    @Operation(summary = "删除活动", description = "删除指定活动",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/{id}")
    @RequiresPermission("affair:manage_own_peak")
    public Result<Void> deleteEvent(@Parameter(description = "活动ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        eventService.deleteEvent(id, userId);
        return Result.success(null);
    }

    @Operation(summary = "获取我的活动", description = "获取当前用户组织的所有活动",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/my")
    @RequiresPermission("affair:view")
    public Result<List<Event>> getMyEvents() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        List<Event> events = eventService.getMyEvents(userId);
        return Result.success(events);
    }

    @Operation(summary = "加入活动", description = "当前用户加入指定活动",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "加入成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "活动已结束/人数已满/重复加入")
    })
    @PostMapping("/{id}/join")
    @RequiresPermission("affair:view")
    public Result<Void> joinEvent(@Parameter(description = "活动ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        eventService.joinEvent(id, userId);
        return Result.success(null);
    }
}
