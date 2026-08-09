package com.example.computerassociation.controller;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateTaskDTO;
import com.example.computerassociation.dto.RejectTaskDTO;
import com.example.computerassociation.entity.Task;
import com.example.computerassociation.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "任务管理", description = "任务发布、查询、审核等接口")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Operation(summary = "获取任务列表", description = "获取已发布的任务列表，支持按难度、状态、关键词筛选",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping
    // 注：数据权限过滤在 TaskServiceImpl.getPublishedTasks 中完成（非全局用户自动过滤本峰或自己发布的任务）
    public Result<List<Task>> getTasks(
            @Parameter(description = "任务难度") @RequestParam(required = false) String difficulty,
            @Parameter(description = "任务状态") @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        List<Task> tasks = taskService.getPublishedTasks(difficulty, status, keyword);
        return Result.success(tasks);
    }

    @Operation(summary = "获取任务详情", description = "根据ID获取任务详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "500", description = "任务不存在")
    })
    @GetMapping("/{id}")
    // 注：数据权限过滤在 TaskServiceImpl.getTaskDetail 中完成
    public Result<Task> getTaskById(@Parameter(description = "任务ID") @PathVariable Long id) {
        Task task = taskService.getTaskDetail(id);
        return Result.success(task);
    }

    @Operation(summary = "发布任务", description = "发布新的悬赏任务，初始状态为审核中",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping
    @RequiresPermission("quest:publish_draft")
    public Result<Task> createTask(@Valid @RequestBody CreateTaskDTO dto) {
        Long userId = UserContext.getUserId();
        Task task = taskService.createTask(userId, dto);
        return Result.success(task, "任务发布成功，等待审核");
    }

    @Operation(summary = "获取待审核任务列表", description = "获取所有状态为审核中的任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/pending")
    @RequiresPermission("quest:review")
    public Result<List<Task>> getPendingTasks() {
        List<Task> tasks = taskService.getPendingTasks();
        return Result.success(tasks);
    }

    @Operation(summary = "审核通过任务", description = "将审核中的任务状态改为等待中",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核通过成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "任务状态错误或不存在")
    })
    @PostMapping("/{id}/approve")
    @RequiresPermission("quest:review")
    public Result<Task> approveTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        Long reviewerId = UserContext.getUserId();
        Task task = taskService.approveTask(id, reviewerId);
        return Result.success(task, "审核通过成功");
    }

    @Operation(summary = "审核驳回任务", description = "将审核中的任务驳回，可填写驳回原因",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核驳回成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "任务状态错误或不存在")
    })
    @PostMapping("/{id}/reject")
    @RequiresPermission("quest:review")
    public Result<Task> rejectTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @RequestBody(required = false) RejectTaskDTO dto) {
        Long reviewerId = UserContext.getUserId();
        String reason = dto != null ? dto.getReason() : null;
        Task task = taskService.rejectTask(id, reviewerId, reason);
        return Result.success(task, "任务已驳回");
    }

    @Operation(summary = "获取我发布的任务", description = "获取当前用户发布的所有任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/my")
    // 注：此接口返回当前用户发布的任务，Service 层已按 publisher_id 过滤
    public Result<List<Task>> getMyTasks() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        List<Task> tasks = taskService.getMyPublishedTasks(userId);
        return Result.success(tasks);
    }

    @Operation(summary = "接受任务", description = "勇者接受委托任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "接受成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "任务状态错误或不存在")
    })
    @PostMapping("/{id}/accept")
    @RequiresPermission("quest:accept")
    public Result<Task> acceptTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        Task task = taskService.acceptTask(id, userId);
        return Result.success(task, "接受委托成功");
    }

    @Operation(summary = "提交任务成果", description = "勇者提交任务成果",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "提交成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "任务状态错误或不存在")
    })
    @PostMapping("/{id}/submit")
    @RequiresPermission("quest:submit")
    public Result<Task> submitTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        String description = body != null ? body.get("description") : null;
        String attachmentUrl = body != null ? body.get("attachmentUrl") : null;
        Task task = taskService.submitTask(id, userId, description, attachmentUrl);
        return Result.success(task, "任务提交成功");
    }

    @Operation(summary = "获取我接取的任务", description = "获取当前用户接取的所有任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/accepted")
    // 注：此接口返回当前用户接取的任务，Service 层已按 completer_id 过滤
    public Result<List<Task>> getMyAcceptedTasks() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }
        List<Task> tasks = taskService.getMyAcceptedTasks(userId);
        return Result.success(tasks);
    }

    @Operation(summary = "编辑任务", description = "编辑任务信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "编辑成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{id}")
    // 注：权限检查在 TaskServiceImpl.updateTask 中完成（区分 quest:edit_any, quest:edit_own_peak 及发布者权限）
    public Result<Task> updateTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @RequestBody CreateTaskDTO dto) {
        Long userId = UserContext.getUserId();
        Task task = taskService.updateTask(id, userId, dto);
        return Result.success(task);
    }

    @Operation(summary = "删除任务", description = "删除指定任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/{id}")
    // 注：权限检查在 TaskServiceImpl.deleteTask 中完成（区分 quest:delete_any, quest:delete_own_peak 及发布者权限）
    public Result<Void> deleteTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        Long userId = UserContext.getUserId();
        taskService.deleteTask(id, userId);
        return Result.success(null);
    }

    @Operation(summary = "验收任务", description = "验收已提交的任务成果，确认完成并发放奖励",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "验收成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "500", description = "任务状态错误或不存在")
    })
    @PostMapping("/{id}/complete")
    @RequiresPermission("quest:review_result")
    public Result<Task> completeTask(@Parameter(description = "任务ID") @PathVariable Long id) {
        Long reviewerId = UserContext.getUserId();
        if (reviewerId == null) {
            return Result.fail(401, "用户未登录");
        }
        Task task = taskService.completeTask(id, reviewerId);
        return Result.success(task, "任务完成，奖励已发放");
    }

    @Operation(summary = "强制结项", description = "管理员强制终止任务",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "强制结项成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/{id}/force-close")
    @RequiresPermission("quest:force_close")
    public Result<Task> forceCloseTask(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String reason = body != null ? body.get("reason") : null;
        Task task = taskService.forceCloseTask(id, userId, reason);
        return Result.success(task);
    }
}
