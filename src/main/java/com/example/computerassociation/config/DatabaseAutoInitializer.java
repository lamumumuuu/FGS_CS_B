// src/main/java/com/example/computerassociation/config/DatabaseAutoInitializer.java

/**
 * 数据库自动初始化器
 * 
 * 在 Spring 容器启动后执行，自动检测并初始化数据库。
 * 执行顺序为 0（通过 @Order(0) 指定），早于 DataInitializer。
 * 
 * 功能：
 * 1. 检测目标数据库是否存在
 * 2. 不存在时自动创建数据库
 * 3. 执行表结构初始化脚本
 * 4. 填充基础数据
 * 
 * 错误处理：
 * - 数据库连接失败
 * - 权限不足
 * - SQL执行错误
 * - 资源耗尽
 */

package com.example.computerassociation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(0)                                /// 控制执行顺序，早于 DataInitializer
public class DatabaseAutoInitializer implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /// SQL 脚本执行顺序
    private static final String[] INIT_SCRIPTS = {
        "sql/Users.sql",
        "sql/rbac_init.sql",
        "sql/task_and_sect_init.sql"
    };

    public DatabaseAutoInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("开始执行数据库存在性检测与初始化...");
        log.info("========================================");

        try {
            /// 1. 检测数据库是否存在
            String dbName = extractDatabaseName(datasourceUrl);
            boolean dbExists = checkDatabaseExists(dbName);

            if (dbExists) {
                log.info("数据库 [{}] 已存在，跳过创建", dbName);
                
                /// 检测表结构是否完整
                if (!checkTablesExist()) {
                    log.warn("数据库存在但表结构不完整，开始初始化表结构...");
                    executeInitScripts();
                } else {
                    log.info("数据库表结构完整，跳过初始化");
                }
            } else {
                log.warn("数据库 [{}] 不存在，开始自动创建...", dbName);
                
                /// 2. 创建数据库
                createDatabase(dbName);
                
                /// 3. 初始化表结构和数据
                executeInitScripts();
                
                log.info("========================================");
                log.info("  数据库初始化成功！");
                log.info("  数据库名: {}", dbName);
                log.info("  字符集: UTF-8");
                log.info("  排序规则: 默认");
                log.info("  表结构: 已创建");
                log.info("  初始数据: 已填充");
                log.info("========================================");
            }
        } catch (SecurityException e) {
            log.error("数据库初始化失败：权限不足");
            log.error("错误详情: {}", e.getMessage());
            log.error("请检查数据库用户权限：需要 CREATEDB 权限");
            throw new RuntimeException("数据库初始化失败：权限不足", e);
        } catch (SQLException e) {
            log.error("数据库初始化失败：SQL 执行错误");
            log.error("SQL 状态: {}", e.getSQLState());
            log.error("错误代码: {}", e.getErrorCode());
            log.error("错误详情: {}", e.getMessage());
            throw new RuntimeException("数据库初始化失败：SQL 执行错误", e);
        } catch (OutOfMemoryError e) {
            log.error("数据库初始化失败：系统资源不足");
            log.error("错误详情: {}", e.getMessage());
            throw new RuntimeException("数据库初始化失败：系统资源不足", e);
        } catch (Exception e) {
            log.error("数据库初始化失败：未知错误");
            log.error("错误详情: {}", e.getMessage(), e);
            throw new RuntimeException("数据库初始化失败：未知错误", e);
        }
    }

    /**
     * 从 JDBC URL 中提取数据库名称
     */
    private String extractDatabaseName(String url) {
        try {
            // 格式: jdbc:postgresql://host:port/dbname
            String[] parts = url.split("/");
            if (parts.length >= 4) {
                String dbPart = parts[parts.length - 1];
                // 处理可能的参数（如 ?useSSL=false）
                return dbPart.split("\\?")[0];
            }
            throw new IllegalArgumentException("无法解析数据库名称: " + url);
        } catch (Exception e) {
            log.error("解析数据库 URL 失败: {}", url);
            throw new RuntimeException("无效的数据库 URL", e);
        }
    }

    /**
     * 检测数据库是否存在
     */
    private boolean checkDatabaseExists(String dbName) throws SQLException {
        log.info("检测数据库 [{}] 是否存在...", dbName);
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 查询所有数据库
            ResultSet catalogs = metaData.getCatalogs();
            List<String> dbNames = new ArrayList<>();
            
            while (catalogs.next()) {
                dbNames.add(catalogs.getString("TABLE_CAT"));
            }
            catalogs.close();
            
            log.debug("现有数据库列表: {}", dbNames);
            return dbNames.contains(dbName);
        }
    }

    /**
     * 检测核心表是否存在
     */
    private boolean checkTablesExist() throws SQLException {
        log.info("检测核心表结构...");
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = "public";
            
            // 检测必需的核心表（含业务扩展表，任一缺失即触发初始化脚本重建）
            String[] requiredTables = {
                "users", "peaks", "roles", "permissions",
                "role_permissions", "user_roles", "disciples", "tasks",
                "events", "event_participants", "announcements",
                "audit_logs", "lingshi_transactions"
            };
            
            for (String tableName : requiredTables) {
                ResultSet tables = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"});
                boolean exists = tables.next();
                tables.close();
                
                if (!exists) {
                    log.debug("核心表 [{}] 不存在", tableName);
                    return false;
                }
            }
            
            log.debug("所有核心表存在");
            return true;
        }
    }

    /**
     * 创建数据库
     */
    private void createDatabase(String dbName) throws SQLException {
        log.info("开始创建数据库 [{}]...", dbName);
        
        // 连接到默认数据库 (postgres)
        String defaultDbUrl = buildDefaultDbUrl();
        log.debug("连接到默认数据库: {}", defaultDbUrl);
        
        try (Connection conn = dataSource.getConnection()) {
            // 注意：PostgreSQL 不支持在事务中创建数据库
            conn.setAutoCommit(true);
            
            try (Statement stmt = conn.createStatement()) {
                // 创建数据库，指定字符集和排序规则
                String createSql = String.format(
                    "CREATE DATABASE \"%s\" " +
                    "ENCODING 'UTF8' " +
                    "LC_COLLATE = 'zh_CN.UTF-8' " +
                    "LC_CTYPE = 'zh_CN.UTF-8' " +
                    "TEMPLATE template0",
                    dbName
                );
                
                log.debug("执行 SQL: {}", createSql);
                stmt.execute(createSql);
                
                log.info("数据库 [{}] 创建成功", dbName);
            } catch (SQLException e) {
                // 如果字符集不支持，尝试使用默认配置
                if (e.getMessage().contains("编码") || e.getMessage().contains("locale")) {
                    log.warn("指定的字符集/排序规则不可用，使用默认配置创建数据库");
                    
                    try (Statement stmt = conn.createStatement()) {
                        String fallbackSql = String.format(
                            "CREATE DATABASE \"%s\" ENCODING 'UTF8'",
                            dbName
                        );
                        log.debug("执行备用 SQL: {}", fallbackSql);
                        stmt.execute(fallbackSql);
                        
                        log.info("数据库 [{}] 创建成功（使用默认配置）", dbName);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * 构建默认数据库 URL（连接到 postgres）
     */
    private String buildDefaultDbUrl() {
        // 从原始 URL 中提取 host 和 port
        // jdbc:postgresql://host:port/dbname -> jdbc:postgresql://host:port/postgres
        return datasourceUrl.replaceAll("/[^/]*$", "/postgres");
    }

    /**
     * 执行初始化 SQL 脚本
     */
    private void executeInitScripts() {
        log.info("开始执行数据库初始化脚本...");
        
        try (Connection conn = dataSource.getConnection()) {
            for (String scriptPath : INIT_SCRIPTS) {
                log.info("执行脚本: {}", scriptPath);
                
                try {
                    ClassPathResource resource = new ClassPathResource(scriptPath);
                    
                    if (!resource.exists()) {
                        log.error("初始化脚本不存在: {}", scriptPath);
                        throw new RuntimeException("初始化脚本缺失: " + scriptPath);
                    }
                    
                    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                    populator.addScript(resource);
                    populator.setContinueOnError(false);  /// 遇到错误立即停止
                    populator.setIgnoreFailedDrops(false);
                    
                    // 执行脚本
                    populator.execute(dataSource);
                    
                    log.info("脚本 [{}] 执行成功", scriptPath);
                } catch (Exception e) {
                    log.error("脚本 [{}] 执行失败: {}", scriptPath, e.getMessage());
                    throw new RuntimeException("初始化脚本执行失败: " + scriptPath, e);
                }
            }
            
            log.info("所有初始化脚本执行完成");
        } catch (SQLException e) {
            log.error("获取数据库连接失败: {}", e.getMessage());
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    /**
     * 验证数据库连接
     */
    private boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);  // 5秒超时
        } catch (SQLException e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库版本信息
     */
    private String getDatabaseVersion() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            log.warn("获取数据库版本失败: {}", e.getMessage());
            return "未知";
        }
    }
}