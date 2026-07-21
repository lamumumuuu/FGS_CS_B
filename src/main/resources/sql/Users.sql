-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY, -- 主键，自增
    username VARCHAR(50) NOT NULL UNIQUE, -- 用户名，唯一且不为空
    email VARCHAR(100) NOT NULL UNIQUE, -- 邮箱，唯一且不为空
    password VARCHAR(100) NOT NULL, -- 加密后的密码，不为空
    avatar VARCHAR(255), -- 头像URL，可以为空
    role INTEGER DEFAULT 0, -- 角色：0-普通用户，1-干事,2-管理员，默认为普通用户
    status INTEGER DEFAULT 1, -- 用户状态：0-禁用，1-启用，默认为启用
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，默认为当前时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 更新时间，默认为当前时间
    last_login_time TIMESTAMP -- 最后登录时间，可以为空
);

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);