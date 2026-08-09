package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.CreateTaskDTO;
import com.example.computerassociation.entity.Task;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.TaskMapper;
import com.example.computerassociation.service.AuditLogService;
import com.example.computerassociation.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private com.example.computerassociation.service.PermissionService permissionService;

    @Override
    @Transactional
    public Task createTask(Long userId, CreateTaskDTO dto) {
        if (userId == null) {
            throw BusinessException.of(401, "用户未登录");
        }

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDifficulty(dto.getDifficulty());
        task.setReward(dto.getReward());
        task.setStatus("审核中");
        task.setPublisherId(userId);
        task.setPeakId(dto.getPeakId());
        task.setTechRequirements(dto.getTechRequirements());

        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline()));
            } catch (Exception e) {
                log.warn("解析截止日期失败: {}", dto.getDeadline());
            }
        }

        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        boolean success = save(task);
        if (success) {
            log.info("任务发布成功，进入审核: taskId={}, title={}, userId={}",
                    task.getId(), task.getTitle(), userId);

            auditLogService.logSuccessWithOperator(
                    userId, null,
                    "发布任务", "quest", "task", task.getId(),
                    null, "title: " + task.getTitle() + ", status: 审核中",
                    UserContext.getIp()
            );
        }

        return task;
    }

    @Override
    public List<Task> getPublishedTasks(String difficulty, String status, String keyword) {
        QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("status", "审核中");

        // 获取当前用户并进行峰过滤
        // 全局访问条件：isGlobalRoleUser 返回 true，或拥有 quest:view_all 权限
        // （双保险：确保大长老等全局角色即使角色判断异常也能查看所有任务）
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "quest:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                queryWrapper.in("peak_id", userPeakIds);
            } else {
                // 非全局用户且无峰关联，则只能看到自己发布的任务
                queryWrapper.eq("publisher_id", userId);
            }
        }

        if (difficulty != null && !difficulty.isEmpty() && !difficulty.equals("全部难度")) {
            queryWrapper.eq("difficulty", difficulty);
        }

        if (status != null && !status.isEmpty() && !status.equals("全部状态")) {
            queryWrapper.eq("status", status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like("title", keyword).or().like("description", keyword));
        }

        queryWrapper.orderByDesc("created_at");

        List<Task> tasks = taskMapper.selectList(queryWrapper);

        for (Task task : tasks) {
            fillPublisherInfo(task);
        }

        return tasks;
    }

    @Override
    public List<Task> getPendingTasks() {
        Long userId = UserContext.getUserId();
        // 全局访问条件：isGlobalRoleUser 返回 true，或拥有 quest:view_all 权限
        boolean isGlobal = userId != null
                && (permissionService.isGlobalRoleUser(userId)
                    || permissionService.hasPermission(userId, "quest:view_all"));
        
        if (isGlobal) {
            return taskMapper.selectPendingTasks();
        } else {
            List<Long> userPeakIds = userId != null ? permissionService.getUserPeakIds(userId) : null;
            QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "审核中");
            
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                queryWrapper.in("peak_id", userPeakIds);
            } else if (userId != null) {
                queryWrapper.eq("publisher_id", userId);
            }
            
            queryWrapper.orderByAsc("created_at");
            return taskMapper.selectList(queryWrapper);
        }
    }

    @Override
    public Task getTaskDetail(Long id) {
        Task task = taskMapper.selectTaskDetailById(id);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }
        
        // 峰级数据权限校验
        // 全局访问条件：isGlobalRoleUser 返回 true，或拥有 quest:view_all 权限
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "quest:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            // 如果任务有峰限制，且用户有峰关联，但不在该峰，则无权限查看
            if (task.getPeakId() != null && userPeakIds != null && !userPeakIds.isEmpty()) {
                if (!userPeakIds.contains(task.getPeakId())) {
                    // 允许查看自己发布或接取的任务
                    if (!task.getPublisherId().equals(userId) && !task.getCompleterId().equals(userId)) {
                        throw BusinessException.of(403, "无权查看该任务");
                    }
                }
            } else if (task.getPeakId() != null) {
                // 用户无峰关联，只能看自己的任务
                if (!task.getPublisherId().equals(userId) && !task.getCompleterId().equals(userId)) {
                    throw BusinessException.of(403, "无权查看该任务");
                }
            }
        }
        
        return task;
    }

    @Override
    @Transactional
    public Task approveTask(Long taskId, Long reviewerId) {
        Task task = getById(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }

        // 检查审核权限
        if (!canReviewTask(task, reviewerId)) {
            throw BusinessException.of(403, "无权审核该任务");
        }

        if (!"审核中".equals(task.getStatus())) {
            throw BusinessException.of("任务状态不是审核中，无法审核");
        }

        task.setStatus("等待中");
        task.setReviewedBy(reviewerId);
        task.setReviewedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        boolean success = updateById(task);
        if (success) {
            log.info("任务审核通过: taskId={}, reviewerId={}", taskId, reviewerId);

            auditLogService.logSuccessWithOperator(
                    reviewerId, null,
                    "审核通过任务", "quest", "task", taskId,
                    "status: 审核中", "status: 等待中",
                    UserContext.getIp()
            );
        }

        return getTaskDetail(taskId);
    }

    /**
     * 检查用户是否有审核该任务的权限
     * 规则：全局角色可审核所有任务；峰级角色只能审核本峰任务；发布者自己不能审核自己的任务
     */
    private boolean canReviewTask(Task task, Long userId) {
        if (task == null || userId == null) return false;
        
        // 发布者自己不能审核
        if (userId.equals(task.getPublisherId())) {
            return false;
        }
        
        // 全局角色可以审核所有任务
        if (permissionService.isGlobalRoleUser(userId)
                || permissionService.hasPermission(userId, "quest:view_all")) {
            return true;
        }
        
        // 峰级角色只能审核本峰任务
        List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
        if (userPeakIds != null && !userPeakIds.isEmpty()) {
            return task.getPeakId() != null && userPeakIds.contains(task.getPeakId());
        }
        
        return false;
    }

    @Override
    @Transactional
    public Task rejectTask(Long taskId, Long reviewerId, String reason) {
        Task task = getById(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }

        // 检查审核权限
        if (!canReviewTask(task, reviewerId)) {
            throw BusinessException.of(403, "无权驳回该任务");
        }

        if (!"审核中".equals(task.getStatus())) {
            throw BusinessException.of("任务状态不是审核中，无法审核");
        }

        task.setStatus("已驳回");
        task.setRejectReason(reason);
        task.setReviewedBy(reviewerId);
        task.setReviewedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        boolean success = updateById(task);
        if (success) {
            log.info("任务审核驳回: taskId={}, reviewerId={}, reason={}",
                    taskId, reviewerId, reason);

            auditLogService.logSuccessWithOperator(
                    reviewerId, null,
                    "审核驳回任务", "quest", "task", taskId,
                    "status: 审核中", "status: 已驳回, reason: " + reason,
                    UserContext.getIp()
            );
        }

        return getTaskDetail(taskId);
    }

    @Override
  public List<Task> getMyPublishedTasks(Long userId) {
    List<Task> tasks = taskMapper.selectTasksByPublisherId(userId);
    return tasks;
  }

  @Override
  @Transactional
  public Task acceptTask(Long taskId, Long userId) {
    Task task = getById(taskId);
    if (task == null) {
      throw BusinessException.of("任务不存在");
    }

    if (!"等待中".equals(task.getStatus())) {
      throw BusinessException.of("任务状态不是等待中，无法接受");
    }

    if (task.getCompleterId() != null) {
      throw BusinessException.of("任务已被其他勇者接取");
    }

    task.setCompleterId(userId);
    task.setStatus("讨伐中");
    task.setUpdatedAt(LocalDateTime.now());

    boolean success = updateById(task);
    if (success) {
      log.info("任务接受成功: taskId={}, completerId={}", taskId, userId);

      auditLogService.logSuccessWithOperator(
          userId, null,
          "接受任务", "quest", "task", taskId,
          "status: 等待中", "status: 讨伐中, completerId: " + userId,
          UserContext.getIp()
      );
    }

    return getTaskDetail(taskId);
  }

  @Override
  @Transactional
  public Task submitTask(Long taskId, Long userId, String description, String attachmentUrl) {
    Task task = getById(taskId);
    if (task == null) {
      throw BusinessException.of("任务不存在");
    }

    if (!"讨伐中".equals(task.getStatus())) {
      throw BusinessException.of("任务状态不是讨伐中，无法提交");
    }

    if (!userId.equals(task.getCompleterId())) {
      throw BusinessException.of("你不是该任务的接受者，无法提交");
    }

    task.setUpdatedAt(LocalDateTime.now());

    boolean success = updateById(task);
    if (success) {
      log.info("任务提交成功: taskId={}, userId={}", taskId, userId);

      auditLogService.logSuccessWithOperator(
          userId, null,
          "提交任务", "quest", "task", taskId,
          "status: 讨伐中", "status: 讨伐中(已提交)",
          UserContext.getIp()
      );
    }

    return getTaskDetail(taskId);
  }

  @Override
  @Transactional
  public Task completeTask(Long taskId, Long reviewerId) {
    Task task = getById(taskId);
    if (task == null) {
      throw BusinessException.of("任务不存在");
    }

    if (!"讨伐中".equals(task.getStatus())) {
      throw BusinessException.of("任务状态不是讨伐中，无法完成");
    }

    task.setStatus("已完成");
    task.setReviewedBy(reviewerId);
    task.setReviewedAt(LocalDateTime.now());
    task.setUpdatedAt(LocalDateTime.now());

    boolean success = updateById(task);
    if (success) {
      log.info("任务完成确认: taskId={}, reviewerId={}", taskId, reviewerId);

      auditLogService.logSuccessWithOperator(
          reviewerId, null,
          "完成任务", "quest", "task", taskId,
          "status: 讨伐中", "status: 已完成",
          UserContext.getIp()
      );
    }

    return getTaskDetail(taskId);
  }

  @Override
  public List<Task> getMyAcceptedTasks(Long userId) {
    QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("completer_id", userId);
    queryWrapper.orderByDesc("created_at");
    List<Task> tasks = taskMapper.selectList(queryWrapper);
    for (Task task : tasks) {
      fillPublisherInfo(task);
    }
    return tasks;
  }

  private void fillPublisherInfo(Task task) {
        if (task.getPublisherId() != null) {
            task.setPublisherName("用户" + task.getPublisherId());
        }
    }

    @Override
    @Transactional
    public Task updateTask(Long id, Long userId, CreateTaskDTO dto) {
        Task task = getById(id);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }

        // 权限检查：任务发布者可以编辑自己的任务，或具备全局编辑权限
        boolean isPublisher = task.getPublisherId() != null && task.getPublisherId().equals(userId);
        boolean hasEditAnyPermission = permissionService.hasPermission(userId, "quest:edit_any");
        
        if (!isPublisher && !hasEditAnyPermission) {
            // 检查是否具备本峰编辑权限
            boolean hasEditOwnPeakPermission = permissionService.hasPermission(userId, "quest:edit_own_peak");
            if (hasEditOwnPeakPermission && task.getPeakId() != null) {
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                if (userPeakIds == null || !userPeakIds.contains(task.getPeakId())) {
                    throw BusinessException.of(403, "无权编辑该任务");
                }
            } else {
                throw BusinessException.of(403, "无权编辑该任务");
            }
        }

        // 更新字段
        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getDifficulty() != null) {
            task.setDifficulty(dto.getDifficulty());
        }
        if (dto.getReward() != null) {
            task.setReward(dto.getReward());
        }
        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline()));
            } catch (Exception e) {
                log.warn("解析截止日期失败: {}", dto.getDeadline());
            }
        }
        if (dto.getTechRequirements() != null) {
            task.setTechRequirements(dto.getTechRequirements());
        }

        task.setUpdatedAt(LocalDateTime.now());

        boolean success = updateById(task);
        if (success) {
            log.info("任务更新成功: taskId={}, userId={}", id, userId);

            auditLogService.logSuccessWithOperator(
                    userId, null,
                    "编辑任务", "quest", "task", id,
                    null, "任务信息已更新",
                    UserContext.getIp()
            );
        }

        return getTaskDetail(id);
    }

    @Override
    @Transactional
    public void deleteTask(Long id, Long userId) {
        Task task = getById(id);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }

        // 权限检查：任务发布者可以删除自己的任务，或具备全局删除权限
        boolean isPublisher = task.getPublisherId() != null && task.getPublisherId().equals(userId);
        boolean hasDeleteAnyPermission = permissionService.hasPermission(userId, "quest:delete_any");
        
        if (!isPublisher && !hasDeleteAnyPermission) {
            // 检查是否具备本峰删除权限
            boolean hasDeleteOwnPeakPermission = permissionService.hasPermission(userId, "quest:delete_own_peak");
            if (hasDeleteOwnPeakPermission && task.getPeakId() != null) {
                List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
                if (userPeakIds == null || !userPeakIds.contains(task.getPeakId())) {
                    throw BusinessException.of(403, "无权删除该任务");
                }
            } else {
                throw BusinessException.of(403, "无权删除该任务");
            }
        }

        String oldStatus = task.getStatus();
        boolean success = removeById(id);
        if (success) {
            log.info("任务删除成功: taskId={}, userId={}", id, userId);

            auditLogService.logSuccessWithOperator(
                    userId, null,
                    "删除任务", "quest", "task", id,
                    "status: " + oldStatus,
                    null,
                    UserContext.getIp()
            );
        }
    }

    @Override
    @Transactional
    public Task forceCloseTask(Long id, Long userId, String reason) {
        Task task = getById(id);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }

        // 只有未完成的任务才能强制结项
        if ("已完成".equals(task.getStatus()) || "已驳回".equals(task.getStatus())) {
            throw BusinessException.of("任务已终结，无法强制结项");
        }

        String oldStatus = task.getStatus();
        task.setStatus("已强制结项");
        task.setRejectReason(reason != null ? reason : "管理员强制结项");
        task.setReviewedBy(userId);
        task.setReviewedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        boolean success = updateById(task);
        if (success) {
            log.info("任务强制结项成功: taskId={}, userId={}, reason={}", id, userId, reason);

            auditLogService.logSuccessWithOperator(
                    userId, null,
                    "强制结项", "quest", "task", id,
                    "status: " + oldStatus, "status: 已强制结项",
                    UserContext.getIp()
            );
        }

        return getTaskDetail(id);
    }
}
