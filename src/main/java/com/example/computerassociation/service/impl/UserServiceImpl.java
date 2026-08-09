// src/main/java/com/example/computerassociation/service/impl/UserServiceImpl.java

/**
 * 用户服务实现类
 * 继承 MyBatis-Plus ServiceImpl 获得基础 CRUD，实现注册、登录、用户名查重等功能。
 * 注册时默认分配外门弟子角色，并自动创建弟子记录。
 * 登录失败会触发 Redis 锁定机制（5次/15分钟）。
 */

package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.Disciple;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.entity.UserRole;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.DiscipleMapper;
import com.example.computerassociation.mapper.UserMapper;
import com.example.computerassociation.mapper.UserRoleMapper;
import com.example.computerassociation.service.AuditLogService;
import com.example.computerassociation.service.RoleService;
import com.example.computerassociation.service.UserService;
import com.example.computerassociation.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private DiscipleMapper discipleMapper;          // 用于自动创建弟子记录

    /* ------------------------------------------------------------------ */
    /*  注册                                                             */
    /* ------------------------------------------------------------------ */

    /** 普通用户注册，默认分配 outer_disciple 角色，并自动创建弟子记录 */
    @Override
    @Transactional
    public boolean register(RegisterDTO registerDTO) {
        // 参数校验
        if (registerDTO == null) throw BusinessException.of("注册信息不能为空");
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty())
            throw BusinessException.of("用户名不能为空");
        if (registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty())
            throw BusinessException.of("密码不能为空");
        if (registerDTO.getPassword().length() < 6) throw BusinessException.of("密码长度不能少于6位");

        if (existsByUsername(registerDTO.getUsername())) throw BusinessException.of("用户名已存在");

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword())); // 密码 BCrypt 加密
        user.setStatus(1);
        user.setRole(2);                                  /// 旧字段，备用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        boolean success = save(user);                     // 插入用户表
        if (success) {
            // 分配默认角色
            Role defaultRole = roleService.getRoleByName("outer_disciple");
            if (defaultRole == null) {
                log.warn("默认角色 outer_disciple 不存在，用户 {} 将无任何角色", user.getUsername());
            } else {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(defaultRole.getId());
                userRole.setPeakId(null);                 // 外门弟子暂不绑定峰
                userRole.setCreateTime(LocalDateTime.now());
                userRoleMapper.insert(userRole);
                log.info("新用户注册，默认分配外门弟子角色: userId={}, username={}", user.getId(), user.getUsername());
            }

            // 自动创建弟子记录
            Disciple disciple = new Disciple();
            disciple.setUserId(user.getId());
            disciple.setName(user.getUsername());
            disciple.setStudentId("");
            disciple.setRole("outer_disciple");

            // 如果前端传了峰，则使用传过来的值；否则默认 "无"
            String peakValue = registerDTO.getPeak();
            if (peakValue == null || peakValue.trim().isEmpty()) {
                peakValue = "无";
            }
            disciple.setPeak(peakValue);

            disciple.setLingshi(0L);
            disciple.setJoinedAt(LocalDateTime.now());
            disciple.setUpdatedAt(LocalDateTime.now());
            disciple.setCreatedAt(LocalDateTime.now());
            discipleMapper.insert(disciple);
            log.info("自动创建弟子记录: userId={}, username={}, role=outer_disciple, peak={}",
                    user.getId(), user.getUsername(), peakValue);

            // 记录审计日志
            auditLogService.logSuccessWithOperator(
                    user.getId(), user.getUsername(),
                    "用户注册", "member", "user", user.getId(),
                    null, "username: " + user.getUsername() + ", role: outer_disciple",
                    UserContext.getIp()
            );
        }
        return success;
    }

    /** 带角色和峰信息的注册（用于 DataInitializer 创建管理员） */
    @Override
    @Transactional
    public boolean registerWithRole(User user, String roleName, Long peakId) {
        if (user == null) throw BusinessException.of("用户信息不能为空");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty())
            throw BusinessException.of("用户名不能为空");
        if (user.getPassword() == null || user.getPassword().trim().isEmpty())
            throw BusinessException.of("密码不能为空");
        if (roleName == null || roleName.trim().isEmpty()) throw BusinessException.of("角色名称不能为空");

        if (existsByUsername(user.getUsername())) throw BusinessException.of("用户名已存在");

        Role role = roleService.getRoleByName(roleName);
        if (role == null) throw BusinessException.of("角色不存在: " + roleName);

        boolean isPeakRole = "elder".equals(roleName) || "inner_disciple".equals(roleName);
        if (isPeakRole && peakId == null) throw BusinessException.of("峰级角色必须指定峰ID");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) user.setStatus(1);
        if (user.getCreateTime() == null) user.setCreateTime(LocalDateTime.now());
        if (user.getUpdateTime() == null) user.setUpdateTime(LocalDateTime.now());

        boolean success = save(user);
        if (success) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRole.setPeakId(peakId);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(userRole);

            log.info("创建用户并分配角色: userId={}, username={}, role={}, peakId={}",
                    user.getId(), user.getUsername(), roleName, peakId);

            // 如果是管理员创建的特殊用户，通常不需要弟子记录，所以这里不创建 Disciple

            auditLogService.logSuccessWithOperator(
                    UserContext.getUserId(), UserContext.getUsername(),
                    "创建用户", "member", "user", user.getId(),
                    null, "username: " + user.getUsername() + ", role: " + roleName + ", peakId: " + peakId,
                    UserContext.getIp()
            );
        }
        return success;
    }

    /* ------------------------------------------------------------------ */
    /*  登录                                                             */
    /* ------------------------------------------------------------------ */

    /** 登录验证，失败锁定，成功更新最后登录时间 */
    @Override
    public User login(String username, String password) {
        // 检查是否被锁定
        if (redisUtil.isLoginLocked(username)) {
            long remainingTime = redisUtil.getLoginLockRemainingTime(username);
            long remainingMinutes = remainingTime / 60;
            throw BusinessException.of("账户已被锁定，请 " + remainingMinutes + " 分钟后重试");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        // 用户不存在或密码不匹配
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            redisUtil.recordLoginFailure(username);      // 记录失败次数
            return null;
        }

        // 检查是否被禁用
        if (user.getStatus() == 0) {
            throw BusinessException.of("账户已被禁用");
        }

        // 登录成功，清除失败记录，更新登录时间
        redisUtil.clearLoginFailure(username);
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    /* ------------------------------------------------------------------ */
    /*  辅助查询                                                         */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean existsByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
}