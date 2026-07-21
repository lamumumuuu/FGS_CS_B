# 计算机协会管理系统后端

> 基于Spring Boot 3.5.10开发的计算机协会用户认证与权限管理系统

## 项目简介

本项目是计算机协会管理系统的后端服务，提供用户注册、登录、密码重置等核心功能。采用现代化的技术栈，遵循RESTful API设计规范，为前端提供稳定、安全的数据接口。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.5.10 | 核心框架 |
| Spring Security | 3.5.10 | 安全框架 |
| MyBatis-Plus | 3.5.13 | ORM框架 |
| PostgreSQL | - | 关系型数据库 |
| Redis | - | 缓存数据库 |
| JWT | 0.11.2 | 令牌认证 |
| Hutool | 5.8.29 | Java工具库 |
| Lombok | - | 简化代码 |
| Liquibase | - | 数据库版本管理 |
| Thymeleaf | - | 模板引擎 |
| SpringDoc | 2.6.0 | API文档 |

## 功能特性

### 用户模块
- 用户注册（邮箱验证码 + 图形验证码）
- 用户登录（支持用户名/邮箱 + 密码）
- 密码重置（邮箱验证）
- 获取用户信息（JWT认证）
- 图形验证码生成

### 安全特性
- BCrypt密码加密
- JWT无状态认证
- 验证码防刷机制
- Spring Security安全配置
- CORS跨域支持

### 工程特性
- 分层架构设计
- 统一异常处理
- 统一响应结果封装
- 完善的日志配置
- 数据库版本管理

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 13+
- Redis 5.0+

### 安装步骤

1. 克隆项目
```bash
git clone <repository-url>
cd fgs2026-01-24-service
```

2. 配置数据库

创建PostgreSQL数据库：
```sql
CREATE DATABASE computer_association;
```

3. 配置环境变量

创建 `.env` 文件或在 `application.yml` 中配置以下环境变量：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/computer_association
    username: postgres
    password: ${DB_PASSWORD}
  
  mail:
    host: smtp.163.com
    port: 465
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400

spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
```

4. 初始化数据库

项目启动时，Liquibase会自动执行数据库迁移脚本，创建 `users` 表。

5. 启动项目

```bash
mvn spring-boot:run
```

或使用Maven Wrapper：
```bash
./mvnw spring-boot:run
```

6. 验证启动

访问以下地址确认服务启动成功：
- API文档：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/actuator/health

## API接口文档

### 基础信息

- Base URL: `http://localhost:8080`
- 认证方式: Bearer Token (JWT)

### 接口列表

#### 1. 获取图形验证码
```http
GET /api/user/captcha
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "captchaKey": "uuid-string",
    "captchaImage": "data:image/png;base64,..."
  }
}
```

#### 2. 发送注册验证码
```http
POST /api/user/send-code
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "验证码已发送至您的邮箱",
  "data": null
}
```

#### 3. 用户注册
```http
POST /api/user/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "user@example.com",
  "password": "password123",
  "captchaKey": "uuid-string",
  "captchaCode": "1234"
}
```

#### 4. 用户登录
```http
POST /api/user/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "username": "testuser",
      "email": "user@example.com",
      "role": 0,
      "status": 1
    }
  }
}
```

#### 5. 获取用户信息
```http
GET /api/user/info
Authorization: Bearer {token}
```

#### 6. 发送重置密码验证码
```http
POST /api/user/send-reset-code
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### 7. 重置密码
```http
PUT /api/user/reset-password
Content-Type: application/json

{
  "email": "user@example.com",
  "newPassword": "newpassword123",
  "verificationCode": "123456"
}
```

## 数据库设计

### users表

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | SERIAL | 主键 | PRIMARY KEY |
| username | VARCHAR(50) | 用户名 | UNIQUE, NOT NULL |
| email | VARCHAR(100) | 邮箱 | UNIQUE, NOT NULL |
| password | VARCHAR(100) | 加密密码 | NOT NULL |
| avatar | VARCHAR(255) | 头像URL | - |
| role | INTEGER | 角色 | DEFAULT 0 |
| status | INTEGER | 状态 | DEFAULT 1 |
| create_time | TIMESTAMP | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | TIMESTAMP | 更新时间 | DEFAULT CURRENT_TIMESTAMP |
| last_login_time | TIMESTAMP | 最后登录时间 | - |

### 角色说明

| role值 | 角色 | 说明 |
|--------|------|------|
| 0 | 普通用户 | 默认角色 |
| 1 | 干事 | 协会干事 |
| 2 | 管理员 | 系统管理员 |

### 状态说明

| status值 | 状态 | 说明 |
|----------|------|------|
| 0 | 禁用 | 账号被禁用 |
| 1 | 启用 | 账号正常 |

## 项目结构

```
src/main/java/com/example/computerassociation/
├── common/                 # 公共类
│   └── Result.java        # 统一响应结果
├── config/                # 配置类
│   ├── CorsConfig.java
│   ├── MyBatisPlusConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/            # 控制器层
│   └── UserController.java
├── dto/                   # 数据传输对象
│   ├── RegisterDTO.java
│   └── UserDTO.java
├── entity/                # 实体类
│   └── User.java
├── exception/             # 异常类
│   └── exception.java
├── mapper/                # 数据访问层
│   └── UserMapper.java
├── service/               # 服务层
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── util/                  # 工具类
│   ├── JwtUtil.java
│   ├── MailUtil.java
│   └── RedisUtil.java
└── ComputerAssociationApplication.java  # 启动类
```

## 配置说明

### application.yml 主配置文件

包含数据库、邮件、JWT等核心配置。

### application-liquibase.yml 数据库迁移配置

启用Liquibase数据库版本管理。

### application-redis.yml Redis配置

Redis连接池配置。

### logback-spring.xml 日志配置

- 控制台输出
- 普通日志文件（app.log）
- 错误日志文件（error.log）
- 日志滚动策略：单文件最大10MB，保留30天

## 开发指南

### 运行测试

```bash
mvn test
```

### 打包部署

```bash
mvn clean package
java -jar target/ComputerAssociation-0.0.1-SNAPSHOT.jar
```

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok减少样板代码
- 统一异常处理
- 接口返回统一Result格式

## 常见问题

### 1. 邮件发送失败

检查以下几点：
- 邮箱是否开启了SMTP服务
- 授权码是否正确（非登录密码）
- 网络是否正常

### 2. Redis连接失败

确认Redis服务是否启动：
```bash
redis-cli ping
```

### 3. 数据库连接失败

检查PostgreSQL服务是否运行，以及连接配置是否正确。

### 4. 跨域问题

项目已配置CORS，如果仍有问题，检查 [SecurityConfig.java](src/main/java/com/example/computerassociation/config/SecurityConfig.java) 和 [CorsConfig.java](src/main/java/com/example/computerassociation/config/CorsConfig.java) 配置。

## 许可证

Copyright © 2026 计算机协会

## 贡献指南

欢迎提交Issue和Pull Request。

## 联系方式

如有问题，请联系项目维护者。
