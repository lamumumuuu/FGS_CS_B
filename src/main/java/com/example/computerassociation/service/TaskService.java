package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.dto.CreateTaskDTO;
import com.example.computerassociation.entity.Task;

import java.util.List;

public interface TaskService extends IService<Task> {

    Task createTask(Long userId, CreateTaskDTO dto);

    List<Task> getPublishedTasks(String difficulty, String status, String keyword);

    List<Task> getPendingTasks();

    Task getTaskDetail(Long id);

    Task approveTask(Long taskId, Long reviewerId);

    Task rejectTask(Long taskId, Long reviewerId, String reason);

    List<Task> getMyPublishedTasks(Long userId);

    Task acceptTask(Long taskId, Long userId);

    Task submitTask(Long taskId, Long userId, String description, String attachmentUrl);

    Task completeTask(Long taskId, Long reviewerId);

    List<Task> getMyAcceptedTasks(Long userId);

    Task updateTask(Long id, Long userId, CreateTaskDTO dto);

    void deleteTask(Long id, Long userId);

    Task forceCloseTask(Long id, Long userId, String reason);
}
