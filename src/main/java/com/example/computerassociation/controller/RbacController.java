// src/main/java/com/example/computerassociation/controller/RbacController.java

/**
 * 权限管理控制器
 * 
 * 提供角色、权限、峰管理的 REST API。
 * 包括当前用户权限查询、角色列表、峰列表、用户角色管理与权限查询。
 * 敏感操作通过 @RequiresPermission 进行细粒度权限校验。
 */

package com.example.computerassociation.controller;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.entity.Permission;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.service.PeakService;
import com.example.computerassociation.service.PermissionService;
import com.example.computerassociation.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "权限管理", description = "角色、权限、峰管理接口")
@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PeakService peakService;

    /* ------------------------------------------------------------------ */
    /*  当前用户权限查询                                                  */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取当前用户权限", description = "获取当前登录用户的所有权限和角色")
    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> getMyPermissions() {
        Long userId = UserContext.getUserId();                          // 从上下文获取当前登录用户 ID
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        List<Role> roles = permissionService.getRolesByUserId(userId);
        List<Long> peakIds = permissionService.getUserPeakIds(userId);
        boolean isGlobal = permissionService.isGlobalRoleUser(userId);

        // 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("permissions", permissions);
        data.put("roles", roles);
        data.put("peakIds", peakIds);
        data.put("isGlobal", isGlobal);

        return Result.success(data);
    }

    /* ------------------------------------------------------------------ */
    /*  全局数据查询：角色列表、峰列表                                    */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取所有角色", description = "获取系统所有角色列表")
    @GetMapping("/roles")
    public Result<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    @Operation(summary = "获取所有峰", description = "获取系统所有峰列表")
    @GetMapping("/peaks")
    public Result<List<Peak>> getAllPeaks() {
        List<Peak> peaks = peakService.getAllPeaks();
        return Result.success(peaks);
    }

    @Operation(summary = "添加新峰", description = "创建新的峰（组织分支）")
    @PostMapping("/peaks")
    @RequiresPermission("peak:create")
    public Result<Peak> addPeak(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        
        if (name == null || name.trim().isEmpty()) {
            return Result.fail(400, "峰名称不能为空");
        }
        
        Peak existing = peakService.getPeakByName(name);
        if (existing != null) {
            return Result.fail(400, "该峰已存在");
        }
        
        Peak peak = peakService.addPeak(name.trim(), description);
        return Result.success(peak, "创建成功");
    }

    @Operation(summary = "删除峰", description = "删除指定的峰（需确保峰下无弟子）")
    @DeleteMapping("/peaks/{id}")
    @RequiresPermission("peak:create")
    public Result<Boolean> deletePeak(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return Result.fail(400, "峰ID无效");
        }
        
        if (peakService.hasMembers(id)) {
            return Result.fail(400, "该峰下还有弟子，无法删除");
        }
        
        boolean success = peakService.deletePeak(id);
        if (!success) {
            return Result.fail(404, "峰不存在");
        }
        return Result.success(true, "删除成功");
    }

    /* ------------------------------------------------------------------ */
    /*  用户角色与权限管理（需认证与特定权限）                              */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取用户角色", description = "获取指定用户的角色列表")
    @GetMapping("/user/{userId}/roles")
    @RequiresPermission("member:view_all")                              // 需要查看所有成员的权限
    public Result<List<Role>> getUserRoles(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.fail(400, "用户ID无效");
        }
        List<Role> roles = roleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @Operation(summary = "更新用户角色", description = "更新指定用户的角色（含权限等级校验，不能修改高于自己等级的用户）")
    @PutMapping("/user/{userId}/roles")
    @RequiresPermission("member:update_role")                           // 需要修改成员角色的权限
    public Result<Void> updateUserRoles(
            @PathVariable Long userId,
            @RequestParam List<Long> roleIds,
            @RequestParam(required = false) Long peakId) {
        if (userId == null || userId <= 0) {
            return Result.fail(400, "用户ID无效");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return Result.fail(400, "角色列表不能为空");
        }

        Long operatorId = UserContext.getUserId();                     // 操作者 ID
        if (operatorId == null) {
            return Result.fail(401, "用户未登录");
        }

        roleService.updateUserRolesWithCheck(operatorId, userId, roleIds, peakId);
        return Result.success(null, "角色更新成功");
    }

    @Operation(summary = "获取用户权限", description = "获取指定用户的权限列表")
    @GetMapping("/user/{userId}/permissions")
    @RequiresPermission("member:view_all")
    public Result<List<Permission>> getUserPermissions(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return Result.fail(400, "用户ID无效");
        }
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        return Result.success(permissions);
    }

    /* ------------------------------------------------------------------ */
    /*  角色权限管理（需认证与特定权限）                                    */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取所有权限", description = "获取系统所有权限列表")
    @GetMapping("/permissions")
    @RequiresPermission("member:view_all")
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }

    @Operation(summary = "获取角色权限", description = "获取指定角色的权限列表")
    @GetMapping("/roles/{roleId}/permissions")
    @RequiresPermission("member:view_all")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
        if (roleId == null || roleId <= 0) {
            return Result.fail(400, "角色ID无效");
        }
        List<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);
        return Result.success(permissions);
    }

    @Operation(summary = "更新角色权限", description = "更新指定角色的权限配置")
    @PutMapping("/roles/{roleId}/permissions")
    @RequiresPermission("system:admin")
    public Result<Void> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds) {
        if (roleId == null || roleId <= 0) {
            return Result.fail(400, "角色ID无效");
        }

        Long operatorId = UserContext.getUserId();
        if (operatorId == null) {
            return Result.fail(401, "用户未登录");
        }

        if (!permissionService.canModifyRolePermissions(operatorId)) {
            return Result.fail(403, "无权修改角色权限");
        }

        permissionService.updateRolePermissions(roleId, permissionIds);
        return Result.success(null, "权限更新成功");
    }
}