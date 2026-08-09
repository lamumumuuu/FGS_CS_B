-- ========================================
-- RBAC 权限管理系统数据库初始化脚本
-- 适用于 PostgreSQL
-- 用途：首次部署时一键创建表结构并填充初始数据（峰、权限、角色及其关联）
-- 
-- 注意：权限标识必须与后端 Java @RequiresPermission 注解
--       和前端 types/permissions.ts 中的常量保持一致
-- ========================================

-- 1. 峰表
CREATE TABLE IF NOT EXISTS peaks (
    id SERIAL PRIMARY KEY,                    -- 自增主键
    name VARCHAR(32) NOT NULL UNIQUE,         -- 峰名，唯一
    description VARCHAR(255),                 -- 描述
    total_lingshi BIGINT DEFAULT 0,           -- 峰累计灵石（历史累计值）
    available_lingshi BIGINT DEFAULT 0,       -- 峰可支配灵石（当前可用余额）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为已存在的 peaks 表添加 lingshi 字段（幂等）
ALTER TABLE peaks ADD COLUMN IF NOT EXISTS total_lingshi BIGINT DEFAULT 0;
ALTER TABLE peaks ADD COLUMN IF NOT EXISTS available_lingshi BIGINT DEFAULT 0;

-- 2. 权限表（细粒度操作权限，如 quest:publish_global）
CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,         -- 权限标识，如 quest:view_all
    display_name VARCHAR(64) NOT NULL,        -- 显示名称
    module VARCHAR(32) NOT NULL,              -- 所属模块：quest/member/affair/peak/finance/system
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 角色表（7 种固定角色：宗主、大长老、太上长老、荣誉长老、长老、内门弟子、外门弟子）
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,         -- 角色标识，如 sect_master
    display_name VARCHAR(32) NOT NULL,        -- 中文显示名
    description VARCHAR(255),
    level SMALLINT NOT NULL,                  -- 角色层级（0=最高，数值越大权限越低）
    is_system SMALLINT DEFAULT 1,             -- 是否系统内置角色（1=是，不可删除）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 角色-权限关联表（多对多）
CREATE TABLE IF NOT EXISTS role_permissions (
    id SERIAL PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,          -- 角色删除时级联删除关联
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)                                        -- 防止重复授权
);

-- 5. 用户-角色关联表（一个用户可拥有多个角色，可指定所属峰）
CREATE TABLE IF NOT EXISTS user_roles (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                  -- 用户 ID（关联用户表，此处未定义用户表结构）
    role_id INT NOT NULL,
    peak_id INT DEFAULT NULL,                 -- 用户在该角色下所属的峰（NULL 表示无峰限制/全局角色）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (peak_id) REFERENCES peaks(id) ON DELETE SET NULL          -- 峰被删除时该字段置空
);

-- 6. 审计日志表（记录所有敏感操作）
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    operator_id BIGINT,                       -- 操作人 ID
    operator_name VARCHAR(64),                -- 操作人名称
    operation VARCHAR(128) NOT NULL,          -- 操作描述
    module VARCHAR(32),                       -- 操作模块
    target_type VARCHAR(64),                  -- 操作目标类型
    target_id BIGINT,                         -- 操作目标 ID
    before_data TEXT,                         -- 操作前数据（JSON 格式）
    after_data TEXT,                          -- 操作后数据（JSON 格式）
    ip_address VARCHAR(64),                   -- 操作 IP
    user_agent VARCHAR(512),                  -- 客户端信息
    status SMALLINT DEFAULT 1,                -- 操作结果（1=成功，0=失败）
    error_message TEXT,                       -- 失败时的错误信息
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. 索引（加速常用查询）
CREATE INDEX IF NOT EXISTS idx_permissions_module ON permissions(module);
CREATE INDEX IF NOT EXISTS idx_roles_level ON roles(level);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_operator_id ON audit_logs(operator_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module ON audit_logs(module);
CREATE INDEX IF NOT EXISTS idx_audit_logs_create_time ON audit_logs(create_time);

-- ========================================
-- 初始化峰数据（5 个峰：管理台 + 3 个技术方向 + _TREASURY_ 特殊峰）
-- ON CONFLICT DO NOTHING 保证幂等，重复执行不会报错
-- _TREASURY_ 特殊峰用于持久化"总可支配灵石"（宗门公共账本）
-- ========================================
INSERT INTO peaks (name, description, available_lingshi, total_lingshi) VALUES
    ('管理台', '协会管理决策中枢', 0, 0),
    ('项目峰', '项目开发与技术攻关', 0, 0),
    ('算法峰', '算法研究与竞赛', 0, 0),
    ('电路峰', '硬件设计与嵌入式开发', 0, 0),
    ('_TREASURY_', '宗门总可支配灵石特殊峰', 100000, 100000)
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- 初始化权限数据（共 38 条，分 6 个模块）
-- 
-- 权限命名规范：module:action
-- - quest:     任务大厅（14 条）
-- - member:    成员管理（6 条）
-- - affair:    宗门事务（6 条）
-- - peak:      峰管理（5 条）
-- - finance:   财务管理（4 条）
-- - system:    系统管理（1 条）
-- ========================================

-- 任务模块权限（14 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('quest:view_all', '查看所有任务', 'quest', '查看全协会所有任务详情'),
    ('quest:view_own_peak', '查看本峰任务', 'quest', '查看本峰相关任务'),
    ('quest:publish_global', '发布全协会悬赏', 'quest', '发布无峰限制的全协会悬赏任务'),
    ('quest:publish_peak', '发布本峰悬赏', 'quest', '发布本峰的悬赏任务'),
    ('quest:publish_draft', '发布任务草稿', 'quest', '发布任务草稿，需峰主/长老审核'),
    ('quest:edit_any', '编辑任意任务', 'quest', '编辑任意任务内容'),
    ('quest:edit_own_peak', '编辑本峰任务', 'quest', '编辑本峰的任务'),
    ('quest:delete_any', '删除任意任务', 'quest', '删除任意任务'),
    ('quest:delete_own_peak', '删除本峰任务', 'quest', '删除本峰的任务'),
    ('quest:accept', '接取任务', 'quest', '接取任务进行完成'),
    ('quest:submit', '提交任务成果', 'quest', '提交任务完成成果'),
    ('quest:review', '审核任务', 'quest', '审核任务草稿（通过/驳回）'),
    ('quest:review_result', '验收任务成果', 'quest', '验收已提交的任务成果'),
    ('quest:force_close', '强制结项', 'quest', '强制关闭/结项任务')
ON CONFLICT (name) DO NOTHING;

-- 成员模块权限（6 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('member:view_all', '查看全协会成员', 'member', '查看全协会成员完整信息'),
    ('member:view_own_peak', '查看本峰成员', 'member', '查看本峰成员信息'),
    ('member:approve_join', '审批入会申请', 'member', '审批新成员入会申请'),
    ('member:update_role', '修改成员角色', 'member', '修改成员角色等级（受角色层级限制）'),
    ('member:appoint_elder', '任命/撤换长老', 'member', '任命或撤换长老（含峰主）'),
    ('member:expel', '开除成员', 'member', '开除协会成员')
ON CONFLICT (name) DO NOTHING;

-- 宗门事务模块权限（6 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('affair:view', '查看宗门事务', 'affair', '查看宗门公告和活动'),
    ('affair:announce_global', '发布宗门公告', 'affair', '发布全协会公告'),
    ('affair:announce_peak', '发布本峰公告', 'affair', '发布本峰公告'),
    ('affair:create_event', '创建活动', 'affair', '创建协会活动'),
    ('affair:manage_all_events', '管理所有活动', 'affair', '管理所有活动'),
    ('affair:manage_own_peak', '管理本峰活动', 'affair', '管理本峰的活动')
ON CONFLICT (name) DO NOTHING;

-- 峰管理模块权限（5 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('peak:view', '查看峰信息', 'peak', '查看峰信息和成员'),
    ('peak:create', '创建/解散峰', 'peak', '创建或解散峰'),
    ('peak:edit_any', '修改任意峰信息', 'peak', '修改任意峰的信息'),
    ('peak:edit_own', '修改本峰信息', 'peak', '修改本峰的信息'),
    ('peak:manage_members', '峰成员管理', 'peak', '向本峰添加/移除成员')
ON CONFLICT (name) DO NOTHING;

-- 财务模块权限（4 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('finance:view_all', '查看全协会财务', 'finance', '查看全协会财务流水'),
    ('finance:view_own_peak', '查看本峰财务', 'finance', '查看本峰相关财务'),
    ('finance:adjust_lingshi', '调整灵石', 'finance', '手动发放/扣除灵石'),
    ('finance:set_base', '设定悬赏基准', 'finance', '设定悬赏基准灵石数量')
ON CONFLICT (name) DO NOTHING;

-- 系统模块权限（1 条）
INSERT INTO permissions (name, display_name, module, description) VALUES
    ('system:admin', '系统管理', 'system', '进入禁地，全站配置、日志、备份等')
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- 初始化角色数据（7 种系统角色）
-- ========================================
INSERT INTO roles (name, display_name, description, level, is_system) VALUES
    ('sect_master', '宗主', '系统唯一超级管理员，当任会长', 0, 1),
    ('grand_elder', '大长老', '协助宗主打理全局，管理权限仅次于宗主，副会长', 1, 1),
    ('supreme_elder', '太上长老', '历届会长/重大贡献管理层，高权限查阅+部分管理权', 1, 1),
    ('honor_elder', '荣誉长老', '其他历届管理层，荣誉称号，主要拥有查阅权', 1, 1),
    ('elder', '长老', '各组组长/其他管理层，有峰级限定，仅能管理自己所在的峰', 1, 1),
    ('inner_disciple', '内门弟子', '各组核心骨干，有峰级限定，可发起草稿、辅助管理本峰', 2, 1),
    ('outer_disciple', '外门弟子', '普通会员，基础权限，仅能浏览任务和接取提交', 2, 1)
ON CONFLICT (name) DO NOTHING;

-- ========================================
-- 初始化角色-权限关联
-- 通过 SELECT 关联查询批量插入，避免硬编码 ID
-- ON CONFLICT DO NOTHING 保证幂等
-- ========================================

-- 宗主（sect_master）：拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'sect_master'
ON CONFLICT DO NOTHING;

-- 大长老（grand_elder）：除 system:admin 外的所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'grand_elder' AND p.name != 'system:admin'
ON CONFLICT DO NOTHING;

-- 太上长老（supreme_elder）：全局查看 + 审批入会 + 任务审核监督
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'supreme_elder' AND p.name IN (
    'quest:view_all', 'quest:view_own_peak', 'quest:review', 'quest:review_result',
    'member:view_all', 'member:view_own_peak', 'member:approve_join',
    'finance:view_all', 'finance:view_own_peak',
    'affair:view', 'peak:view'
)
ON CONFLICT DO NOTHING;

-- 荣誉长老（honor_elder）：纯查阅
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'honor_elder' AND p.name IN (
    'quest:view_all', 'member:view_all', 'finance:view_all',
    'affair:view', 'peak:view'
)
ON CONFLICT DO NOTHING;

-- 长老/峰主（elder）：本峰管理
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'elder' AND p.name IN (
    'quest:view_own_peak', 'quest:publish_peak', 'quest:edit_own_peak',
    'quest:delete_own_peak', 'quest:accept', 'quest:submit',
    'quest:review', 'quest:force_close',
    'member:view_own_peak', 'member:update_role',
    'affair:announce_peak', 'affair:create_event', 'affair:manage_own_peak',
    'peak:view', 'peak:edit_own', 'peak:manage_members',
    'finance:view_own_peak'
)
ON CONFLICT DO NOTHING;

-- 内门弟子（inner_disciple）：本峰查看 + 起草 + 接取提交任务
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'inner_disciple' AND p.name IN (
    'quest:view_own_peak', 'quest:publish_draft', 'quest:accept', 'quest:submit',
    'member:view_own_peak'
)
ON CONFLICT DO NOTHING;

-- 外门弟子（outer_disciple）：基础权限（查看本峰任务、接取、提交）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'outer_disciple' AND p.name IN (
    'quest:view_own_peak', 'quest:accept', 'quest:submit'
)
ON CONFLICT DO NOTHING;
