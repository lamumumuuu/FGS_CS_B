一、角色定义
角色标识
显示名称
对应角色
级别
说明

sect_master
宗主
当任会长
最高
系统唯一超级管理员

grand_elder
大长老
副会长
长老级
协助宗主打理全局
管理权限仅次于宗主

supreme_elder
太上长老
历届会长 / 重大贡献管理层
长老级
拥有高权限查阅
部分管理权

honor_elder
荣誉长老
其他历届管理层
长老级
荣誉称号，主要拥有查阅权

elder
长老（峰主）
各组组长 / 其他管理层
长老级
有峰级限定，仅能管理自己所在的峰

inner_disciple
内门弟子
各组核心骨干
弟子级
有峰级限定，可发起草稿、辅助管理本峰

outer_disciple
外门弟子
普通会员
弟子级
基础权限，仅能浏览任务和接取提交

---
二、数据库表结构
1. 权限表 permissions
sql
CREATE TABLE permissions (    id INT PRIMARY KEY AUTO_INCREMENT,    name VARCHAR(64) NOT NULL UNIQUE,   -- 权限标识，如 quest:review    display_name VARCHAR(64) NOT NULL,  -- 中文说明    module VARCHAR(32) NOT NULL         -- 所属模块：quest, member, affair, peak, finance, system);
2. 角色表 roles
sql
CREATE TABLE roles (    id INT PRIMARY KEY AUTO_INCREMENT,    name VARCHAR(32) NOT NULL UNIQUE,   -- 角色标识    display_name VARCHAR(32) NOT NULL,  -- 显示名称    description VARCHAR(255),    level TINYINT NOT NULL,             -- 层级：0=最高，1=长老级，2=弟子级    is_system TINYINT DEFAULT 1         -- 是否系统内置（禁止删除）);
3. 角色-权限关联表 role_permissions
sql
CREATE TABLE role_permissions (    role_id INT NOT NULL,    permission_id INT NOT NULL,    PRIMARY KEY (role_id, permission_id),    FOREIGN KEY (role_id) REFERENCES roles(id),    FOREIGN KEY (permission_id) REFERENCES permissions(id));
4. 用户-角色关联表 user_roles
sql
CREATE TABLE user_roles (    user_id INT NOT NULL,    role_id INT NOT NULL,    peak_id INT DEFAULT NULL,           -- 仅峰主、内门弟子必填    PRIMARY KEY (user_id, role_id),    FOREIGN KEY (role_id) REFERENCES roles(id),    FOREIGN KEY (peak_id) REFERENCES peaks(id));
• peak_id 为 NULL 表示该角色为全局角色，不受峰限制。
• 一个用户可拥有多个角色（如某人既是荣誉长老，又兼任某峰长老）。
5. 峰表 peaks
sql
CREATE TABLE peaks (    id INT PRIMARY KEY AUTO_INCREMENT,    name VARCHAR(32) NOT NULL);
---
三、完整权限定义
沿用 资源:操作 命名规范。
模块
权限标识
说明

任务
quest:view_all
查看所有任务详情


quest:view_own_peak
查看本峰任务（内门以上可用）


quest:create_global
发布全协会悬赏（无峰限制）


quest:create_peak
发布本峰悬赏


quest:create_draft
发布任务草稿（需峰主/长老审核）


quest:edit_any
编辑任意任务


quest:edit_own_peak
编辑本峰任务


quest:delete_any
删除任意任务


quest:delete_own_peak
删除本峰任务


quest:accept
接取任务


quest:submit
提交任务成果


quest:review
审核/验收任务


quest:force_close
强制结项

成员
member:view_all
查看全协会成员完整信息


member:view_own_peak
查看本峰成员信息


member:approve_join
审批入会申请


member:update_role
修改成员角色（受角色层级限制）


member:appoint_elder
任命/撤换长老（含峰主）


member:remove
开除成员

宗门事务
affair:announce_global
发布宗门公告


affair:announce_peak
发布本峰公告


affair:event_create
创建活动


affair:event_manage
管理所有活动

峰管理
peak:create
创建/解散峰


peak:edit_any
修改任意峰信息


peak:edit_own
修改本峰信息


peak:add_member
向本峰添加/移除成员

财务
finance:view_all
查看全协会财务流水


finance:view_own_peak
查看本峰相关财务


finance:adjust
手动发放/扣除灵石


finance:set_base
设定悬赏基准灵石

系统
system:admin
进入禁地（全站配置、日志、备份）

---
四、角色权限矩阵
权限标识
宗主
大长老
太上长老
荣誉长老
长老(峰主)
内门弟子
外门弟子

任务








quest:view_all
✓
✓
✓
✓
–
–
–

quest:view_own_peak
✓
✓
✓
–
✓
✓
✓(仅公开)

quest:create_global
✓
✓
–
–
–
–
–

quest:create_peak
✓
✓
–
–
✓
–
–

quest:create_draft
–
–
–
–
–
✓
–

quest:edit_any
✓
✓
–
–
–
–
–

quest:edit_own_peak
✓
✓
–
–
✓
–
–

quest:delete_any
✓
✓
–
–
–
–
–

quest:delete_own_peak
✓
✓
–
–
✓
–
–

quest:accept
✓
✓
–
–
✓
✓
✓

quest:submit
✓
✓
–
–
✓
✓
✓

quest:review
✓
✓
✓(仅监督)
–
✓(本峰)
–
–

quest:force_close
✓
✓
–
–
✓(本峰)
–
–

成员








member:view_all
✓
✓
✓
✓
–
–
–

member:view_own_peak
✓
✓
✓
–
✓
✓(简化)
–

member:approve_join
✓
✓
✓
–
–
–
–

member:update_role
✓
✓(≤长老)
–
–
仅内门
–
–

member:appoint_elder
✓
✓
–
–
–
–
–

member:remove
✓
✓
–
–
–
–
–

宗门事务








affair:announce_global
✓
✓
–
–
–
–
–

affair:announce_peak
✓
✓
–
–
✓
–
–

affair:event_create
✓
✓
–
–
✓(本峰)
–
–

affair:event_manage
✓
✓
–
–
–
–
–

峰管理








peak:create
✓
✓
–
–
–
–
–

peak:edit_any
✓
✓
–
–
–
–
–

peak:edit_own
✓
✓
–
–
✓
–
–

peak:add_member
✓
✓
–
–
✓
–
–

财务








finance:view_all
✓
✓
✓
✓
–
–
–

finance:view_own_peak
✓
✓
✓
–
✓
–
–

finance:adjust
✓
✓
–
–
–
–
–

finance:set_base
✓
✓
–
–
–
–
–

系统








system:admin
✓
–
–
–
–
–
–

说明：
• 太上长老的角色定位是“德高望重的监督者”，拥有全局查看权、审批入会权，以及任务审核的监督权（可查看任意审核中的任务并给出意见，但最终决定由宗主/大长老执行，此处简化设计为同样拥有 quest:review，但可限定为仅查看不可操作，按需调整）。
• 荣誉长老是纯查阅角色，仅能查看公开和内部信息，无任何操作权。
• 内门弟子可发布草稿，协助管理本峰成员（仅限查看本峰成员简单信息，无权修改角色）。
• 峰主的所有操作均限制在 peak_id 等于自己关联峰的数据范围内。
---
五、数据范围限制的实现
1. 登录时加载用户峰信息
    查询 user_roles，若角色为 elder 或 inner_disciple，则提取 peak_id 列表存入会话（如 user_peaks: [1]）。若同时拥有全局角色，则全局角色优先，可绕过峰限制。
2. 权限校验中间件
    后端实现一个中间件，接受权限标识和请求的资源ID，分两步检查：
    ◦ 检查用户是否拥有该权限（通过角色权限表）。
    ◦ 若权限名包含 _own_peak 或用户当前活跃角色是峰级角色，且请求的资源表含有 peak_id 字段，则校验 resource.peak_id IN user_peaks。
        如果用户同时拥有全局角色（如大长老），则直接放行。
3. 全协会资源处理
    对于无峰限制的任务/公告（peak_id IS NULL），峰主的 user_peaks 列表不包含 NULL，自动拦截，从而保证峰主只能操作本峰资源。
---