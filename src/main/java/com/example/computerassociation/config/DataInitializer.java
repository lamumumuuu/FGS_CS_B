// src/main/java/com/example/computerassociation/config/DataInitializer.java

/**
 * 应用启动初始化器
 * 
 * 在 Spring 容器启动后执行，自动检查并创建默认管理员账号。
 * 执行顺序为 1（通过 @Order(1) 指定），早于其他 CommandLineRunner。
 */

package com.example.computerassociation.config;

import com.example.computerassociation.entity.Role;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.service.RoleService;
import com.example.computerassociation.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)                                /// 控制执行顺序，数字越小越先执行
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;     /// 用户服务

    @Autowired
    private RoleService roleService;     /// 角色服务

    @Override
    public void run(String... args) {
        log.info("开始执行数据初始化...");
        initAdminUser();                 /// 初始化管理员账号
        log.info("数据初始化完成");
    }

    /**
     * 初始化根账号（宗主 admin）
     * 如果账号已存在则跳过，否则使用默认密码 admin123 创建，
     * 并赋予 sect_master 角色。
     */
    private void initAdminUser() {
        String adminUsername = "admin";
        String adminPassword = "admin123";
        String adminRoleName = "sect_master";

        try {
            // 检查管理员账号是否已存在
            User existingUser = userService.getByUsername(adminUsername);
            if (existingUser != null) {
                log.info("根账号已存在，跳过初始化: {}", adminUsername);
                return;
            }

            // 确保系统角色已通过 SQL 脚本初始化
            Role adminRole = roleService.getRoleByName(adminRoleName);
            if (adminRole == null) {
                log.error("根角色 {} 不存在，请先执行 rbac_init.sql 初始化数据", adminRoleName);
                log.warn("根账号初始化失败：缺少角色数据，将在下次启动时重试");
                return;
            }

            log.info("正在初始化根账号: {} (密码: {})", adminUsername, adminPassword);

            // 创建管理员用户并直接分配宗主角色
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setPassword(adminPassword);
            adminUser.setStatus(1);          /// 状态正常
            adminUser.setRole(0);            /// 角色字段暂用 0 表示默认

            boolean success = userService.registerWithRole(adminUser, adminRoleName, null);

            if (success) {
                log.info("========================================");
                log.info("  根账号初始化成功！");
                log.info("  用户名: {}", adminUsername);
                log.info("  初始密码: {}", adminPassword);
                log.info("  角色: 宗主 ({})", adminRoleName);
                log.info("  【重要】请首次登录后立即修改密码！");
                log.info("========================================");
            } else {
                log.error("根账号创建失败");
            }
        } catch (Exception e) {
            log.error("根账号初始化异常: {}", e.getMessage(), e);
        }
    }
}