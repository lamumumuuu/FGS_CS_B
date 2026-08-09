// src/main/java/com/example/computerassociation/mapper/PermissionMapper.java

/**
 * 权限 Mapper 接口
 * 包含根据用户 ID 或角色 ID 查询权限的自定义 SQL。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 查询用户拥有的所有权限（通过 user_roles → role_permissions 关联） */
    @Select("SELECT p.* FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> selectByUserId(@Param("userId") Long userId);

    /** 查询某个角色拥有的权限 */
    @Select("SELECT p.* FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    /** 查询用户拥有的权限名称列表（仅 name 字段） */
    @Select("SELECT p.name FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionNamesByUserId(@Param("userId") Long userId);

    /** 查询角色拥有的权限名称列表（仅 name 字段） */
    @Select("SELECT p.name FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<String> selectPermissionNamesByRoleId(@Param("roleId") Long roleId);
}