// src/main/java/com/example/computerassociation/service/PermissionService.java

/**
 * 权限与角色查询服务接口
 * 提供细粒度的权限判断、角色判断及相关查询功能。
 */

package com.example.computerassociation.service;

import com.example.computerassociation.entity.Permission;
import com.example.computerassociation.entity.Role;

import java.util.List;

public interface PermissionService {

    /** 获取用户的所有权限对象 */
    List<Permission> getPermissionsByUserId(Long userId);

    /** 获取用户的权限名称列表（如 quest:view_all） */
    List<String> getPermissionNamesByUserId(Long userId);

    /** 获取用户的所有角色对象 */
    List<Role> getRolesByUserId(Long userId);

    /** 获取用户的角色名称列表（如 sect_master） */
    List<String> getUserRoleNames(Long userId);

    /** 判断用户是否拥有某权限 */
    boolean hasPermission(Long userId, String permissionName);

    /** 判断用户是否拥有某角色 */
    boolean hasRole(Long userId, String roleName);

    /** 判断用户是否拥有任一给定角色 */
    boolean hasAnyRole(Long userId, String... roleNames);

    /** 获取用户所属的峰 ID 列表 */
    List<Long> getUserPeakIds(Long userId);

    /** 判断用户是否拥有全局角色（如宗主、大长老，不绑定特定峰） */
    boolean isGlobalRoleUser(Long userId);

    /** 获取所有权限列表 */
    List<Permission> getAllPermissions();

    /** 获取角色的权限列表 */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /** 获取角色的权限名称列表 */
    List<String> getPermissionNamesByRoleId(Long roleId);

    /** 更新角色权限（先删除再插入） */
    void updateRolePermissions(Long roleId, List<Long> permissionIds);

    /** 检查是否可以修改角色权限（仅最高级别角色可修改） */
    boolean canModifyRolePermissions(Long operatorId);
}