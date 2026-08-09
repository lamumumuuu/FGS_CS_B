// src/main/java/com/example/computerassociation/controller/UserController.java

/**
 * 用户管理控制器
 * 
 * 提供用户注册、登录及当前用户信息查询的 REST API。
 * 登录和注册为公开接口，获取用户信息需要携带有效的 JWT Token。
 */

package com.example.computerassociation.controller;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.LoginDTO;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.Disciple;
import com.example.computerassociation.entity.Permission;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.mapper.DiscipleMapper;
import com.example.computerassociation.service.PermissionService;
import com.example.computerassociation.service.UserService;
import com.example.computerassociation.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录等接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;           /// 用户业务服务

    @Autowired
    private JwtUtil jwtUtil;                   /// JWT 工具类

    @Autowired
    private PermissionService permissionService; /// 权限查询服务

    @Autowired
    private DiscipleMapper discipleMapper;        /// 弟子数据访问（用于查询灵石）

    /**
     * 根据用户ID获取关联弟子的灵石数量
     * 灵石数据存储在 disciples 表中，通过 user_id 关联 users 表
     */
    private Long getLingshiByUserId(Long userId) {
        try {
            Disciple disciple = discipleMapper.selectOne(
                    new QueryWrapper<Disciple>().eq("user_id", userId));
            if (disciple != null && disciple.getLingshi() != null) {
                return disciple.getLingshi();
            }
        } catch (Exception e) {
            log.warn("查询用户灵石失败: userId={}", userId, e);
        }
        return 0L;
    }

    /* ------------------------------------------------------------------ */
    /*  用户注册：公开接口，默认分配外门弟子角色                          */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "用户注册", description = "使用用户名和密码注册新用户，默认分配外门弟子角色")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或用户名已存在")
    })
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        boolean success = userService.register(registerDTO);
        return success ? Result.success("注册成功") : Result.fail("注册失败");
    }

    /* ------------------------------------------------------------------ */
    /*  用户登录：验证凭据，返回 JWT Token 及完整权限信息                 */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回token、用户信息、角色和权限")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功，返回JWT令牌和权限信息"),
            @ApiResponse(responseCode = "400", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        if (user != null) {
            String token = jwtUtil.generateToken(user.getUsername());     // 生成 JWT

            // 查询用户的角色、权限、峰信息
            List<Role> roles = permissionService.getRolesByUserId(user.getId());
            List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
            List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());
            List<String> permissionNames = permissions.stream().map(Permission::getName).collect(Collectors.toList());
            List<Long> peakIds = permissionService.getUserPeakIds(user.getId());
            boolean isGlobal = permissionService.isGlobalRoleUser(user.getId());

            // 组装返回数据（与前端 LoginResponse 接口对应）
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            user.setPassword(null);                                       // 安全起见清除密码

            // 构建用户信息 Map，包含灵石数据
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("role", user.getRole());
            userInfo.put("status", user.getStatus());
            userInfo.put("createTime", user.getCreateTime());
            userInfo.put("updateTime", user.getUpdateTime());
            userInfo.put("lastLoginTime", user.getLastLoginTime());
            userInfo.put("lingshi", getLingshiByUserId(user.getId()));

            data.put("user", userInfo);
            data.put("roles", roles);
            data.put("roleNames", roleNames);
            data.put("permissions", permissions);
            data.put("permissionNames", permissionNames);
            data.put("peakIds", peakIds);
            data.put("isGlobal", isGlobal);

            return Result.success(data, "登录成功");
        }
        return Result.fail("用户名或密码错误");
    }

    /* ------------------------------------------------------------------ */
    /*  获取当前用户信息：需携带有效 JWT，返回完整权限                    */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取用户信息", description = "通过JWT令牌获取当前用户信息",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "令牌无效或已过期")
    })
    @GetMapping("/me")
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = UserContext.getUserId();                            // 从 JWT 过滤器设置的上下文中获取
        if (userId == null) {
            return Result.fail(401, "用户未登录或令牌已过期");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }

        if (user.getStatus() == 0) {                                      // 0 表示禁用状态
            return Result.fail(403, "账户已被禁用");
        }

        user.setPassword(null);

        // 查询权限信息（与登录接口返回结构一致）
        List<Role> roles = permissionService.getRolesByUserId(userId);
        List<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        List<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toList());
        List<String> permissionNames = permissions.stream().map(Permission::getName).collect(Collectors.toList());
        List<Long> peakIds = permissionService.getUserPeakIds(userId);
        boolean isGlobal = permissionService.isGlobalRoleUser(userId);

        // 构建用户信息 Map，包含灵石数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        userInfo.put("createTime", user.getCreateTime());
        userInfo.put("updateTime", user.getUpdateTime());
        userInfo.put("lastLoginTime", user.getLastLoginTime());
        userInfo.put("lingshi", getLingshiByUserId(userId));

        Map<String, Object> data = new HashMap<>();
        data.put("user", userInfo);
        data.put("roles", roles);
        data.put("roleNames", roleNames);
        data.put("permissions", permissions);
        data.put("permissionNames", permissionNames);
        data.put("peakIds", peakIds);
        data.put("isGlobal", isGlobal);

        return Result.success(data);
    }

    @Operation(summary = "用户登出", description = "清除服务端状态（当前为无状态JWT，仅做日志记录）",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登出成功")
    })
    @PostMapping("/logout")
    public Result<String> logout() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            log.info("用户登出: userId={}", userId);
        }
        return Result.success("登出成功");
    }

    @Operation(summary = "刷新Token", description = "使用refresh token获取新的access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "400", description = "refresh token无效")
    })
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.fail(400, "refresh token不能为空");
        }
        
        try {
            String username = jwtUtil.getUsernameFromToken(refreshToken);
            if (username == null) {
                return Result.fail(400, "无效的refresh token");
            }
            
            User user = userService.getByUsername(username);
            if (user == null) {
                return Result.fail(400, "用户不存在");
            }
            
            String newToken = jwtUtil.generateToken(username);
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("refreshToken", refreshToken);
            
            return Result.success(data, "Token刷新成功");
        } catch (Exception e) {
            log.warn("Token刷新失败: {}", e.getMessage());
            return Result.fail(400, "refresh token无效");
        }
    }
}