-- Users.sql
-- 用户表结构定义（PostgreSQL）
-- 用于存储系统用户的基本信息，包括用户名、密码、状态等。

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,                        -- 自增主键
    username VARCHAR(50) NOT NULL UNIQUE,         -- 用户名，唯一
    password VARCHAR(100) NOT NULL,               -- 加密后的密码
    avatar VARCHAR(255),                          -- 头像 URL 或路径
    role INTEGER DEFAULT 0,                       -- 角色标识（数字型，0 可能表示默认角色，外键关联 roles 表）
    status INTEGER DEFAULT 1,                     -- 用户状态（1=正常，0=禁用等）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    last_login_time TIMESTAMP                     -- 最后登录时间
);

-- 添加索引，加速用户名和状态查询
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);