// src/main/java/com/example/computerassociation/service/RoleService.java

/**
 * 角色管理服务接口
 * 提供角色的增删改查及用户角色分配功能，包含权限层级校验。
 */

package com.example.computerassociation.service;

import com.example.computerassociation.entity.Role;

import java.util.List;

public interface RoleService {

    /** 获取所有角色 */
    List<Role> getAllRoles();

    /** 按 ID 获取角色 */
    Role getRoleById(Long id);

    /** 按名称获取角色 */
    Role getRoleByName(String name);

    /** 获取用户的角色列表 */
    List<Role> getRolesByUserId(Long userId);

    /** 为用户分配角色（指定峰） */
    void assignRoleToUser(Long userId, Long roleId, Long peakId);

    /** 移除用户的某个角色 */
    void removeRoleFromUser(Long userId, Long roleId);

    /** 更新用户角色列表 */
    void updateUserRoles(Long userId, List<Long> roleIds, Long peakId);

    /** 更新用户角色（含权限层级校验，操作者不能修改高于自己等级的用户） */
    void updateUserRolesWithCheck(Long operatorId, Long targetUserId, List<Long> roleIds, Long peakId);

    /** 判断操作者是否有权限修改目标角色 */
    boolean canModifyRoles(Long operatorId, List<Long> targetRoleIds);

    /** 获取用户最高的角色层级（数值越小权限越高） */
    int getHighestRoleLevel(Long userId);

    /** 获取用户最低的角色层级（数值越大权限越低） */
    int getLowestRoleLevel(Long userId);
}