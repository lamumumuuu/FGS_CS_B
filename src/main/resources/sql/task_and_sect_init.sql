-- ========================================
-- 任务系统数据库初始化脚本
-- 适用于 PostgreSQL
-- 用途：创建任务表和完善弟子表结构
-- ========================================

-- 1. 弟子表（如果不存在则创建）
CREATE TABLE IF NOT EXISTS disciples (
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(64) NOT NULL,
    student_id VARCHAR(32) DEFAULT '',
    role VARCHAR(32) NOT NULL DEFAULT 'outer_disciple',
    peak VARCHAR(32) DEFAULT '无',
    lingshi BIGINT DEFAULT 0,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 弟子表索引
CREATE INDEX IF NOT EXISTS idx_disciples_user_id ON disciples(user_id);
CREATE INDEX IF NOT EXISTS idx_disciples_name ON disciples(name);
CREATE INDEX IF NOT EXISTS idx_disciples_peak ON disciples(peak);
CREATE INDEX IF NOT EXISTS idx_disciples_role ON disciples(role);

-- 2. 任务表
CREATE TABLE IF NOT EXISTS tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(16) NOT NULL DEFAULT '黑铁',
    status VARCHAR(16) NOT NULL DEFAULT '审核中',
    reward INTEGER NOT NULL DEFAULT 0,
    deadline TIMESTAMP,
    publisher_id BIGINT,
    completer_id BIGINT,
    tech_requirements TEXT,
    peak_id INTEGER,
    reject_reason TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 任务表索引
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_difficulty ON tasks(difficulty);
CREATE INDEX IF NOT EXISTS idx_tasks_publisher_id ON tasks(publisher_id);
CREATE INDEX IF NOT EXISTS idx_tasks_completer_id ON tasks(completer_id);
CREATE INDEX IF NOT EXISTS idx_tasks_peak_id ON tasks(peak_id);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at);
CREATE INDEX IF NOT EXISTS idx_tasks_title ON tasks(title);

-- 3. 灵石流水表（记录所有灵石增减变动）
CREATE TABLE IF NOT EXISTS lingshi_transactions (
    id SERIAL PRIMARY KEY,
    disciple_id BIGINT,                       -- 弟子ID（系统级操作可为空）
    disciple_name VARCHAR(64),                -- 弟子姓名（冗余存储）
    type VARCHAR(32) NOT NULL,                -- 变更类型：reward/task_reward/adjust_in/adjust_out/allocate_in/peak_transfer_in/peak_transfer_out
    amount BIGINT NOT NULL,                   -- 变更金额（正数增加，负数减少）
    balance BIGINT,                           -- 变更后余额
    operator_id BIGINT,                       -- 操作人ID
    operator_name VARCHAR(64),                -- 操作人姓名
    remark TEXT,                              -- 备注
    peak_id BIGINT,                           -- 关联峰ID（峰级分类统计用）
    peak_name VARCHAR(32),                    -- 关联峰名称（冗余存储）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 灵石流水表索引
CREATE INDEX IF NOT EXISTS idx_lingshi_transactions_disciple_id ON lingshi_transactions(disciple_id);
CREATE INDEX IF NOT EXISTS idx_lingshi_transactions_type ON lingshi_transactions(type);
CREATE INDEX IF NOT EXISTS idx_lingshi_transactions_peak_id ON lingshi_transactions(peak_id);
CREATE INDEX IF NOT EXISTS idx_lingshi_transactions_created_at ON lingshi_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_lingshi_transactions_operator_id ON lingshi_transactions(operator_id);

-- 灵石流水表注释更新：type 字段支持的类型说明
-- reward: 任务奖励
-- task_reward: 任务奖励（兼容旧类型）
-- adjust_in: 灵石增加（管理员调整）
-- adjust_out: 灵石扣除（管理员调整）
-- allocate_in: 灵石分配（总可支配 → 峰）
-- peak_transfer_in: 峰间调拨收入
-- peak_transfer_out: 峰间调拨支出
-- task_publish: 任务发布扣除灵石
-- task_accept: 任务接取扣除灵石

-- 4. 公告表（宗门公告）
CREATE TABLE IF NOT EXISTS announcements (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,              -- 公告标题
    content TEXT NOT NULL,                    -- 公告内容
    publisher_id BIGINT,                      -- 发布者ID
    publisher_name VARCHAR(64),               -- 发布者名称（冗余存储）
    peak_id BIGINT,                           -- 关联峰ID（全局公告为NULL）
    type VARCHAR(16) NOT NULL DEFAULT 'global', -- 公告类型：global-全协会, peak-本峰
    status VARCHAR(16) NOT NULL DEFAULT 'published', -- 状态：published-已发布, draft-草稿
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 公告表索引
CREATE INDEX IF NOT EXISTS idx_announcements_peak_id ON announcements(peak_id);
CREATE INDEX IF NOT EXISTS idx_announcements_type ON announcements(type);
CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON announcements(created_at);

-- 5. 活动表（宗门活动）
CREATE TABLE IF NOT EXISTS events (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,               -- 活动名称
    description TEXT,                         -- 活动描述
    location VARCHAR(255),                     -- 活动地点
    start_time TIMESTAMP,                     -- 开始时间
    end_time TIMESTAMP,                       -- 结束时间
    organizer_id BIGINT,                      -- 组织方ID
    organizer_name VARCHAR(64),               -- 组织方名称（冗余存储）
    peak_id BIGINT,                           -- 关联峰ID（全局活动为NULL）
    type VARCHAR(16) NOT NULL DEFAULT 'global', -- 活动类型：global-全协会, peak-本峰
    status VARCHAR(16) NOT NULL DEFAULT 'planned', -- 状态：planned-已规划, ongoing-进行中, completed-已结束, cancelled-已取消
    max_participants INTEGER DEFAULT 50,      -- 最大参与人数
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 活动表索引
CREATE INDEX IF NOT EXISTS idx_events_peak_id ON events(peak_id);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at);

-- 6. 初始化一些示例任务数据（可选，用于测试）
INSERT INTO tasks (title, description, difficulty, status, reward, publisher_id, tech_requirements, created_at, updated_at)
VALUES
    ('修复登录页面样式问题', '修复登录页面在移动端显示不正常的问题，需要适配各种屏幕尺寸。', '黑铁', '等待中', 50, 1, 'CSS, Responsive Design', NOW(), NOW()),
    ('开发用户中心页面', '开发完整的用户中心页面，包括个人信息编辑、头像上传等功能。', '青铜', '等待中', 200, 1, 'React, Next.js, TypeScript', NOW(), NOW()),
    ('优化数据库查询性能', '对核心业务查询进行性能优化，添加必要的索引，提升系统响应速度。', '白银', '等待中', 500, 1, 'PostgreSQL, SQL Optimization', NOW(), NOW()),
    ('实现RBAC权限系统', '设计并实现基于角色的访问控制系统，支持细粒度权限控制。', '黄金', '讨伐中', 1000, 1, 'Spring Security, JWT, MyBatis', NOW(), NOW())
ON CONFLICT DO NOTHING;
