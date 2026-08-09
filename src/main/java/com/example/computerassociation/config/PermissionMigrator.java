package com.example.computerassociation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(2)
public class PermissionMigrator implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("开始执行权限数据迁移...");
        try {
            renamePermissions();
            insertMissingPermissions();
            updateRolePermissionAssociations();
            log.info("权限数据迁移完成");
        } catch (Exception e) {
            log.error("权限数据迁移失败: {}", e.getMessage(), e);
        }
    }

    private void renamePermissions() {
        String[][] renames = {
                {"member:remove", "member:expel"},
                {"affair:event_create", "affair:create_event"},
                {"affair:event_manage", "affair:manage_all_events"},
                {"affair:manage_event", "affair:manage_all_events"},
                {"peak:add_member", "peak:manage_members"},
                {"finance:adjust", "finance:adjust_lingshi"},
                {"quest:create_global", "quest:publish_global"},
                {"quest:create_peak", "quest:publish_peak"},
        };

        for (String[] pair : renames) {
            String oldName = pair[0];
            String newName = pair[1];
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE permissions SET name = ?, display_name = ? WHERE name = ?",
                        newName, getDisplayName(newName), oldName);
                if (updated > 0) {
                    log.info("重命名权限: {} -> {}", oldName, newName);
                }
            } catch (Exception e) {
                log.warn("重命名权限失败 {} -> {}: {}", oldName, newName, e.getMessage());
            }
        }
    }

    private void insertMissingPermissions() {
        String[][] missingPermissions = {
                {"affair:view", "查看活动", "affair", "查看协会活动"},
                {"peak:view", "查看峰信息", "peak", "查看峰信息和成员"},
                {"quest:review_result", "审核任务结果", "quest", "审核任务结果"},
                {"quest:publish_draft", "发布草稿任务", "quest", "发布草稿任务"},
        };

        for (String[] perm : missingPermissions) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO permissions (name, display_name, module, description) VALUES (?, ?, ?, ?) ON CONFLICT (name) DO NOTHING",
                        perm[0], perm[1], perm[2], perm[3]);
                log.info("插入权限: {}", perm[0]);
            } catch (Exception e) {
                log.warn("插入权限失败 {}: {}", perm[0], e.getMessage());
            }
        }
    }

    private void updateRolePermissionAssociations() {
        grantPermissionsToRole("sect_master", Arrays.asList(
                "member:view_all", "member:view_own_peak", "member:approve_join",
                "member:update_role", "member:appoint_elder", "member:expel",
                "affair:view", "affair:announce_global", "affair:announce_peak",
                "affair:create_event", "affair:manage_event",
                "peak:view", "peak:create", "peak:edit_any", "peak:edit_own",
                "peak:manage_members",
                "quest:view_all", "quest:view_own_peak", "quest:publish_global",
                "quest:publish_peak", "quest:publish_draft", "quest:edit_any",
                "quest:edit_own_peak", "quest:delete_any", "quest:delete_own_peak",
                "quest:accept", "quest:submit", "quest:review", "quest:review_result",
                "quest:force_close",
                "finance:view_all", "finance:view_own_peak", "finance:adjust_lingshi",
                "finance:set_base",
                "system:admin"
        ));

        grantPermissionsToRole("grand_elder", Arrays.asList(
                "member:view_all", "member:view_own_peak", "member:approve_join",
                "member:update_role", "member:appoint_elder", "member:expel",
                "affair:view", "affair:announce_global", "affair:announce_peak",
                "affair:create_event", "affair:manage_all_events", "affair:manage_own_peak",
                "peak:view", "peak:create", "peak:edit_any", "peak:edit_own",
                "peak:manage_members",
                "quest:view_all", "quest:view_own_peak", "quest:publish_global",
                "quest:publish_peak", "quest:publish_draft", "quest:edit_any",
                "quest:edit_own_peak", "quest:delete_any", "quest:delete_own_peak",
                "quest:accept", "quest:submit", "quest:review", "quest:review_result",
                "quest:force_close",
                "finance:view_all", "finance:view_own_peak", "finance:adjust_lingshi",
                "finance:set_base"
        ));

        grantPermissionsToRole("elder", Arrays.asList(
                "member:view_own_peak", "member:approve_join", "member:update_role",
                "affair:view", "affair:announce_peak", "affair:manage_own_peak",
                "peak:view", "peak:edit_own", "peak:manage_members",
                "quest:view_own_peak", "quest:publish_peak", "quest:edit_own_peak",
                "quest:delete_own_peak", "quest:accept", "quest:submit",
                "quest:review", "quest:force_close",
                "finance:view_own_peak"
        ));

        grantPermissionsToRole("inner_disciple", Arrays.asList(
                "quest:view_own_peak", "quest:publish_draft", "quest:submit"
        ));

        grantPermissionsToRole("outer_disciple", Arrays.asList(
                "quest:view_own_peak", "quest:publish_draft", "quest:submit"
        ));
    }

    private void grantPermissionsToRole(String roleName, List<String> permissionNames) {
        try {
            Long roleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM roles WHERE name = ?", Long.class, roleName);
            if (roleId == null) {
                log.warn("角色不存在: {}", roleName);
                return;
            }

            for (String permName : permissionNames) {
                try {
                    Long permId = jdbcTemplate.queryForObject(
                            "SELECT id FROM permissions WHERE name = ?", Long.class, permName);
                    if (permId != null) {
                        jdbcTemplate.update(
                                "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                roleId, permId);
                    }
                } catch (Exception e) {
                    log.debug("授权跳过: role={}, perm={}, error={}", roleName, permName, e.getMessage());
                }
            }
            log.info("已更新角色 {} 的权限关联 ({} 条)", roleName, permissionNames.size());
        } catch (Exception e) {
            log.error("更新角色 {} 权限失败: {}", roleName, e.getMessage());
        }
    }

    private String getDisplayName(String permissionName) {
        return switch (permissionName) {
            case "member:expel" -> "开除成员";
            case "affair:create_event" -> "创建活动";
            case "affair:manage_all_events" -> "管理所有活动";
            case "peak:manage_members" -> "峰成员管理";
            case "finance:adjust_lingshi" -> "调整灵石";
            case "quest:publish_global" -> "发布全局任务";
            case "quest:publish_peak" -> "发布峰级任务";
            default -> permissionName;
        };
    }
}