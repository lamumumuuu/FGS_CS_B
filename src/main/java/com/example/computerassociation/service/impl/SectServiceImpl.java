package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.AuditLog;
import com.example.computerassociation.entity.Disciple;
import com.example.computerassociation.entity.LingshiTransaction;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.entity.UserRole;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.AuditLogMapper;
import com.example.computerassociation.mapper.DiscipleMapper;
import com.example.computerassociation.mapper.LingshiTransactionMapper;
import com.example.computerassociation.mapper.PeakMapper;
import com.example.computerassociation.mapper.UserMapper;
import com.example.computerassociation.mapper.UserRoleMapper;
import com.example.computerassociation.service.AuditLogService;
import com.example.computerassociation.service.PeakService;
import com.example.computerassociation.service.RoleService;
import com.example.computerassociation.service.SectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SectServiceImpl extends ServiceImpl<DiscipleMapper, Disciple> implements SectService {

    @Autowired
    private DiscipleMapper discipleMapper;

    @Autowired
    private PeakMapper peakMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PeakService peakService;

    @Autowired
    private com.example.computerassociation.service.PermissionService permissionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private LingshiTransactionMapper lingshiTransactionMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    // 管理层角色（包含英文标识和中文显示名，兼容历史数据中 disciples.role 可能存储中文的情况）
    private static final List<String> MANAGEMENT_ROLES = Arrays.asList(
            "sect_master", "grand_elder", "supreme_elder", "honor_elder", "elder",
            "宗主", "大长老", "太上长老", "荣誉长老", "长老"
    );

    @Override
    public List<Disciple> getAllDisciples() {
        QueryWrapper<Disciple> queryWrapper = new QueryWrapper<>();
        
        // 峰级数据过滤
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "member:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                // 获取用户所在峰的名称列表
                QueryWrapper<com.example.computerassociation.entity.Peak> peakQuery = new QueryWrapper<>();
                peakQuery.in("id", userPeakIds);
                List<com.example.computerassociation.entity.Peak> peaks = peakMapper.selectList(peakQuery);
                List<String> peakNames = peaks.stream().map(com.example.computerassociation.entity.Peak::getName).collect(java.util.stream.Collectors.toList());
                
                if (!peakNames.isEmpty()) {
                    queryWrapper.in("peak", peakNames);
                } else {
                    queryWrapper.eq("peak", "无");
                }
            } else {
                queryWrapper.eq("peak", "无");
            }
        }
        
        queryWrapper.orderByDesc("created_at");
        return discipleMapper.selectList(queryWrapper);
    }

    @Override
    public List<Disciple> getManagementDisciples() {
        QueryWrapper<Disciple> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("role", MANAGEMENT_ROLES);
        
        // 峰级数据过滤
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "member:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                QueryWrapper<com.example.computerassociation.entity.Peak> peakQuery = new QueryWrapper<>();
                peakQuery.in("id", userPeakIds);
                List<com.example.computerassociation.entity.Peak> peaks = peakMapper.selectList(peakQuery);
                List<String> peakNames = peaks.stream().map(com.example.computerassociation.entity.Peak::getName).collect(java.util.stream.Collectors.toList());
                
                if (!peakNames.isEmpty()) {
                    queryWrapper.in("peak", peakNames);
                } else {
                    queryWrapper.eq("peak", "无");
                }
            } else {
                queryWrapper.eq("peak", "无");
            }
        }
        
        queryWrapper.orderByAsc("role");
        return discipleMapper.selectList(queryWrapper);
    }

    @Override
    public List<Disciple> getDisciplesByPeak(String peak) {
        if (peak == null || peak.isEmpty() || "全部".equals(peak)) {
            return getAllDisciples();
        }

        QueryWrapper<Disciple> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("peak", peak);
        queryWrapper.orderByDesc("created_at");
        return discipleMapper.selectList(queryWrapper);
    }

    @Override
    public List<Disciple> searchDisciples(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDisciples();
        }

        QueryWrapper<Disciple> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(w -> w.like("name", keyword).or().like("student_id", keyword));
        
        // 峰级数据过滤
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "member:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                QueryWrapper<com.example.computerassociation.entity.Peak> peakQuery = new QueryWrapper<>();
                peakQuery.in("id", userPeakIds);
                List<com.example.computerassociation.entity.Peak> peaks = peakMapper.selectList(peakQuery);
                List<String> peakNames = peaks.stream().map(com.example.computerassociation.entity.Peak::getName).collect(java.util.stream.Collectors.toList());
                
                if (!peakNames.isEmpty()) {
                    queryWrapper.in("peak", peakNames);
                } else {
                    queryWrapper.eq("peak", "无");
                }
            } else {
                queryWrapper.eq("peak", "无");
            }
        }
        
        queryWrapper.orderByDesc("created_at");
        return discipleMapper.selectList(queryWrapper);
    }

    @Override
    public Disciple getDiscipleById(Long id) {
        Disciple disciple = discipleMapper.selectById(id);
        if (disciple == null) {
            throw BusinessException.of("弟子不存在");
        }
        
        // 峰级数据权限校验
        Long userId = UserContext.getUserId();
        if (userId != null && !permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "member:view_all")) {
            List<Long> userPeakIds = permissionService.getUserPeakIds(userId);
            if (userPeakIds != null && !userPeakIds.isEmpty()) {
                QueryWrapper<com.example.computerassociation.entity.Peak> peakQuery = new QueryWrapper<>();
                peakQuery.in("id", userPeakIds);
                List<com.example.computerassociation.entity.Peak> peaks = peakMapper.selectList(peakQuery);
                List<String> peakNames = peaks.stream().map(com.example.computerassociation.entity.Peak::getName).collect(java.util.stream.Collectors.toList());
                
                if (!peakNames.contains(disciple.getPeak())) {
                    throw BusinessException.of(403, "无权查看该弟子信息");
                }
            } else if (!"无".equals(disciple.getPeak())) {
                throw BusinessException.of(403, "无权查看该弟子信息");
            }
        }
        
        return disciple;
    }

    @Override
    @Transactional
    public Disciple addDisciple(Disciple disciple) {
        if (disciple.getName() == null || disciple.getName().trim().isEmpty()) {
            throw BusinessException.of("弟子姓名不能为空");
        }

        disciple.setJoinedAt(LocalDateTime.now());
        disciple.setUpdatedAt(LocalDateTime.now());
        disciple.setCreatedAt(LocalDateTime.now());

        if (disciple.getLingshi() == null) {
            disciple.setLingshi(0L);
        }

        if (disciple.getRole() == null || disciple.getRole().isEmpty()) {
            disciple.setRole("outer_disciple");
        }

        if (disciple.getPeak() == null || disciple.getPeak().isEmpty()) {
            disciple.setPeak("无");
        }

        if (disciple.getUserId() == null) {
            User user = new User();
            user.setUsername(disciple.getName());
            user.setPassword(passwordEncoder.encode("123456"));
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);

            disciple.setUserId(user.getId());
            log.info("自动创建用户账号: username={}, userId={}", user.getUsername(), user.getId());
        }

        boolean success = save(disciple);
        if (success) {
            log.info("添加新弟子: id={}, name={}, role={}, peak={}",
                    disciple.getId(), disciple.getName(), disciple.getRole(), disciple.getPeak());

            if (disciple.getUserId() != null) {
                syncUserRoleAndPeak(null, disciple);
            }

            auditLogService.logSuccessWithOperator(
                    UserContext.getUserId(), UserContext.getUsername(),
                    "添加弟子", "member", "disciple", disciple.getId(),
                    null, "name: " + disciple.getName() + ", role: " + disciple.getRole(),
                    UserContext.getIp()
            );
        }

        return disciple;
    }

    @Override
    @Transactional
    public boolean updateDisciple(Disciple disciple) {
        if (disciple.getId() == null) {
            throw BusinessException.of("弟子ID不能为空");
        }

        // 1. 获取现有弟子信息（旧状态）
        Disciple existing = getDiscipleById(disciple.getId());

        // 2. 保存更新前的关键信息，用于同步对比
        Disciple oldDisciple = new Disciple();
        oldDisciple.setUserId(existing.getUserId());
        oldDisciple.setRole(existing.getRole());
        oldDisciple.setPeak(existing.getPeak());

        // 3. 更新时间戳
        disciple.setUpdatedAt(LocalDateTime.now());

        // 4. 执行更新（MyBatis-Plus 的 updateById 只更新非 null 字段）
        boolean success = updateById(disciple);

        if (success) {
            log.info("更新弟子信息: id={}, name={}", disciple.getId(), disciple.getName());

            // 5. 从数据库获取更新后的完整弟子信息（确保所有字段都有值）
            Disciple updatedDisciple = discipleMapper.selectById(disciple.getId());

            // 6. 同步更新 RBAC 系统中的用户角色和峰
            if (updatedDisciple.getUserId() != null) {
                syncUserRoleAndPeak(oldDisciple, updatedDisciple);
            }

            // 7. 记录审计日志
            auditLogService.logSuccessWithOperator(
                    UserContext.getUserId(), UserContext.getUsername(),
                    "更新弟子信息", "member", "disciple", disciple.getId(),
                    "name: " + existing.getName() + ", role: " + existing.getRole(),
                    "name: " + updatedDisciple.getName() + ", role: " + updatedDisciple.getRole(),
                    UserContext.getIp()
            );
        }

        return success;
    }

    /**
     * 同步更新 RBAC 系统中的用户角色和峰关联
     * 当弟子关联了用户（userId != null），且角色或峰发生变化时，同步更新 user_roles 表
     * 支持新增场景（oldDisciple 为 null 时直接创建角色关联）
     */
    private void syncUserRoleAndPeak(Disciple oldDisciple, Disciple newDisciple) {
        Long userId = newDisciple.getUserId();
        if (userId == null) {
            log.debug("弟子未关联用户，跳过角色同步: discipleId={}", newDisciple.getId());
            return;
        }

        String oldRole = oldDisciple != null ? oldDisciple.getRole() : null;
        String newRole = newDisciple.getRole();
        String oldPeak = oldDisciple != null ? oldDisciple.getPeak() : null;
        String newPeak = newDisciple.getPeak();

        // 判断是否需要同步：新增场景（oldDisciple 为 null）或角色/峰发生变化
        boolean isNew = oldDisciple == null;
        boolean roleChanged = (newRole != null && !newRole.equals(oldRole));
        boolean peakChanged = (newPeak != null && !newPeak.equals(oldPeak));

        if (!isNew && !roleChanged && !peakChanged) {
            log.debug("角色和峰未发生变化，跳过同步: userId={}", userId);
            return;
        }

        try {
            // 获取新角色对应的 Role 实体
            Role roleEntity = roleService.getRoleByName(newRole);
            if (roleEntity == null) {
                // 角色不存在属于数据异常，抛出异常以阻止脏数据写入（事务将回滚）
                log.error("角色不存在，同步失败: roleName={}", newRole);
                throw BusinessException.of("角色不存在: " + newRole + "，无法同步用户角色关联");
            }

            // 获取峰 ID（如果是峰级角色且峰存在）
            Long peakId = null;
            if (newPeak != null && !"无".equals(newPeak) && !newPeak.isEmpty()) {
                Peak peak = peakService.getPeakByName(newPeak);
                if (peak != null) {
                    peakId = peak.getId();
                }
            }

            // 更新用户角色（使用带层级校验的方法，防止权限提升）
            List<Long> roleIds = Collections.singletonList(roleEntity.getId());
            roleService.updateUserRolesWithCheck(UserContext.getUserId(), userId, roleIds, peakId);

            log.info("同步更新用户角色成功: userId={}, oldRole={}, newRole={}, oldPeak={}, newPeak={}",
                    userId, oldRole, newRole, oldPeak, newPeak);

        } catch (BusinessException e) {
            // 业务异常（如角色不存在）直接向上传播，触发事务回滚
            throw e;
        } catch (Exception e) {
            // 其他异常也向上传播，确保数据一致性（避免弟子已写入但角色关联缺失）
            log.error("同步更新用户角色失败: userId={}, newRole={}", userId, newRole, e);
            throw BusinessException.of("角色同步失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteDisciple(Long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw BusinessException.of("删除原因不能为空");
        }

        Disciple disciple = getDiscipleById(id);

        boolean success = removeById(id);
        if (success) {
            log.info("删除弟子: id={}, name={}, reason={}", id, disciple.getName(), reason);

            if (disciple.getUserId() != null) {
                Long userId = disciple.getUserId();

                QueryWrapper<UserRole> userRoleQuery = new QueryWrapper<>();
                userRoleQuery.eq("user_id", userId);
                userRoleMapper.delete(userRoleQuery);

                QueryWrapper<LingshiTransaction> lingshiQuery = new QueryWrapper<>();
                lingshiQuery.eq("operator_id", userId)
                        .or()
                        .eq("disciple_id", disciple.getId());
                lingshiTransactionMapper.delete(lingshiQuery);

                userMapper.deleteById(userId);
                log.info("级联删除用户账号: userId={}, reason={}", userId, reason);
            }

            auditLogService.logSuccessWithOperator(
                    UserContext.getUserId(), UserContext.getUsername(),
                    "删除弟子", "member", "disciple", id,
                    "name: " + disciple.getName() + ", role: " + disciple.getRole(),
                    "reason: " + reason,
                    UserContext.getIp()
            );
        }

        return success;
    }

    @Override
    @Transactional
    public boolean moveDisciplePeak(Long discipleId, String newPeak) {
        Disciple disciple = getDiscipleById(discipleId);

        // 在修改前保存旧状态，用于同步对比
        Disciple oldDisciple = new Disciple();
        oldDisciple.setUserId(disciple.getUserId());
        oldDisciple.setRole(disciple.getRole());
        oldDisciple.setPeak(disciple.getPeak());

        String oldPeak = disciple.getPeak();

        disciple.setPeak(newPeak);
        disciple.setUpdatedAt(LocalDateTime.now());

        boolean success = updateById(disciple);
        if (success) {
            log.info("移动弟子门派: id={}, name={}, from={}, to={}",
                    discipleId, disciple.getName(), oldPeak, newPeak);

            // 同步更新 RBAC 系统中的峰关联
            syncUserRoleAndPeak(oldDisciple, disciple);

            auditLogService.logSuccessWithOperator(
                    UserContext.getUserId(), UserContext.getUsername(),
                    "移动弟子门派", "member", "disciple", discipleId,
                    "peak: " + oldPeak,
                    "peak: " + newPeak,
                    UserContext.getIp()
            );
        }

        return success;
    }

    @Override
  public List<Disciple> filterDisciplesByPeak(String peak) {
    return getDisciplesByPeak(peak);
  }

  @Override
  public List<AuditLog> getDiscipleHistory(Long discipleId) {
      QueryWrapper<AuditLog> queryWrapper = new QueryWrapper<>();
      queryWrapper.eq("target_type", "disciple")
              .eq("target_id", discipleId)
              .orderByDesc("create_time");
      return auditLogMapper.selectList(queryWrapper);
  }

  @Override
  public List<AuditLog> getAllDiscipleHistory(int page, int size) {
      QueryWrapper<AuditLog> queryWrapper = new QueryWrapper<>();
      queryWrapper.eq("target_type", "disciple")
              .orderByDesc("create_time");
      queryWrapper.last("LIMIT " + (page - 1) * size + ", " + size);
      return auditLogMapper.selectList(queryWrapper);
  }
}
