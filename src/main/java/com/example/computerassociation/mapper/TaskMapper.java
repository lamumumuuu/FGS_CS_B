package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT t.*, u.username as publisher_name, u.avatar as publisher_avatar " +
            "FROM tasks t LEFT JOIN users u ON t.publisher_id = u.id " +
            "WHERE t.status != '审核中' " +
            "ORDER BY t.created_at DESC")
    List<Task> selectPublishedTasks();

    @Select("SELECT t.*, u.username as publisher_name, u.avatar as publisher_avatar " +
            "FROM tasks t LEFT JOIN users u ON t.publisher_id = u.id " +
            "WHERE t.status = '审核中' " +
            "ORDER BY t.created_at ASC")
    List<Task> selectPendingTasks();

    @Select("SELECT t.*, u.username as publisher_name, u.avatar as publisher_avatar, " +
            "cu.username as completer_name, cu.avatar as completer_avatar " +
            "FROM tasks t " +
            "LEFT JOIN users u ON t.publisher_id = u.id " +
            "LEFT JOIN users cu ON t.completer_id = cu.id " +
            "WHERE t.id = #{id}")
    Task selectTaskDetailById(@Param("id") Long id);

    @Select("SELECT t.*, u.username as publisher_name, u.avatar as publisher_avatar " +
            "FROM tasks t LEFT JOIN users u ON t.publisher_id = u.id " +
            "WHERE t.publisher_id = #{userId} " +
            "ORDER BY t.created_at DESC")
    List<Task> selectTasksByPublisherId(@Param("userId") Long userId);
}
