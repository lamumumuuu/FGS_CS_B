# 后端连接文档

## 概述

本文档描述 fgs 计算机协会官网前端与后端 API 的连接规范、调用方法和数据格式。

## 目录结构

```
app/api/
├── client/
│   └── index.ts          # 统一的 API 客户端调用封装（含 JWT 认证）
└── README.md             # 本文档
```

## API 基础配置

- **基础 URL**: `http://localhost:8080/api`
- **数据格式**: JSON (application/json)
- **字符编码**: UTF-8
- **认证方式**: JWT (JSON Web Token)

## JWT 认证机制

### Token 存储

- Access Token 存储在 `localStorage` 的 `fgscs_token` 键中
- Refresh Token 存储在 `localStorage` 的 `fgscs_refresh_token` 键中

### 认证流程

1. 用户登录成功后，后端返回 Access Token 和 Refresh Token
2. 前端将 Token 存储在 localStorage 中
3. 需要认证的 API 请求自动在 Header 中添加 `Authorization: Bearer <token>`
4. Token 过期时自动使用 Refresh Token 刷新
5. 刷新失败时清除本地 Token 并触发重新登录

### Token 刷新

- 检测到 Access Token 过期时，自动调用 `/user/refresh` 接口刷新
- 刷新成功后更新本地存储的 Token
- 刷新失败后清除所有 Token，需要用户重新登录

### 辅助方法

```typescript
import { userApi } from "@/app/api/client";

// 检查是否已认证
userApi.isAuthenticated();

// 获取当前 Token
userApi.getToken();

// 手动设置 Token
userApi.setToken(token);

// 清除 Token（退出登录时自动调用）
userApi.clearTokens();
```

## 通用请求封装

### request 函数

所有 API 请求通过统一的 `request<T>` 函数发送，自动处理：
- 请求头设置（Content-Type: application/json）
- JWT Token 自动注入（需要认证的接口）
- Token 过期自动刷新和重试
- 错误状态码抛出
- JSON 响应解析

```typescript
async function request<T>(
  endpoint: string,
  options?: RequestInit,
  requiresAuth?: boolean
): Promise<T>
```

## API 模块划分

### 1. 用户认证模块 (userApi)

| 方法 | 端点 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/user/me` | 获取当前登录用户信息 | 是 |
| POST | `/user/login` | 用户登录 | 否 |
| POST | `/user/register` | 用户注册 | 否 |
| POST | `/user/logout` | 用户退出登录 | 是 |
| POST | `/user/refresh` | 刷新 Access Token | 否 |

#### 登录响应格式

登录和注册成功后，后端应返回：

```typescript
{
  token: string;           // Access Token
  refreshToken: string;    // Refresh Token（可选）
  user: User;              // 用户信息
}
```

#### 用户数据结构 (User)

```typescript
interface User {
  id: string;
  username: string;
  studentId: string;
  avatar?: string;
  role: SectRole | null;       // 职位：宗主/大长老/太上长老/荣誉长老/长老/弟子/null
  peak: SectPeak | null;       // 所属峰：管理台/项目峰/算法峰/电路峰/null
  title: string | null;        // 称号
  lingshi: number;             // 灵石数量
  joinDate: string;            // 加入日期 (YYYY-MM-DD)
  isLoggedIn: boolean;         // 是否登录
  permissions: Permission[];   // 权限列表
}
```

#### 登录请求

```typescript
interface LoginCredentials {
  username: string;
  password: string;
}
```

#### 注册请求

```typescript
interface RegisterData {
  username: string;
  password: string;
  studentId: string;
}
```

### 2. 任务大厅模块 (taskApi)

| 方法 | 端点 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/tasks` | 获取任务列表（支持筛选） | 否 |
| GET | `/tasks/:id` | 获取任务详情 | 否 |

#### 任务数据结构 (Task)

```typescript
interface Task {
  id: string;
  title: string;
  description: string;
  difficulty: TaskDifficulty;  // 黑铁/青铜/白银/黄金
  status: TaskStatus;          // 审核中/等待中/讨伐中/已完成
  reward: number;              // 悬赏灵石数
  deadline: string;            // 截止日期 (YYYY-MM-DD)
  publisher: TaskPublisher;    // 发布者信息
  createdAt: string;           // 发布日期 (YYYY-MM-DD)
  techRequirements: string[];  // 技术需求列表
  completer?: TaskCompleter;   // 完成者（已完成任务有此字段）
}
```

#### 任务列表查询参数

```
GET /tasks?difficulty=黄金&status=等待中&keyword=首页
```

| 参数 | 类型 | 说明 |
|------|------|------|
| difficulty | string | 难度筛选：黑铁/青铜/白银/黄金（可选） |
| status | string | 状态筛选：审核中/等待中/讨伐中/已完成（可选） |
| keyword | string | 关键词搜索，匹配标题和描述（可选） |

### 3. 宗门事务模块 (sectApi)

| 方法 | 端点 | 说明 | 需认证 |
|------|------|------|--------|
| GET | `/sect/current-user` | 获取当前用户的宗门信息 | 是 |
| GET | `/sect/disciples` | 获取弟子列表（支持按峰筛选） | 否 |
| GET | `/sect/disciples/management` | 获取管理层弟子列表 | 否 |
| GET | `/sect/disciples/search` | 搜索弟子 | 否 |
| GET | `/sect/peaks` | 获取所有山峰信息 | 否 |
| POST | `/sect/disciples` | 添加弟子 | 是 |
| PUT | `/sect/disciples/:id/move` | 移动弟子门派 | 是 |
| DELETE | `/sect/disciples/:id` | 删除弟子 | 是 |
| POST | `/sect/disciples/:id/reward` | 打赏弟子 | 是 |

#### 弟子数据结构 (Disciple)

```typescript
interface Disciple {
  id: string;
  name: string;
  studentId: string;
  role: SectRole;             // 角色
  peak: SectPeak;             // 所属峰
  avatar?: string;
  joinedAt: string;           // 加入日期
}
```

#### 山峰数据结构 (PeakInfo)

```typescript
interface PeakInfo {
  name: SectPeak;             // 山峰名称
  description: string;        // 描述
  leaderId?: string;          // 峰主ID
  memberCount: number;        // 成员数量
}
```

#### 权限类型 (Permission)

| 权限标识 | 说明 |
|----------|------|
| `manage_permissions` | 设置弟子权限 |
| `move_disciple` | 移动弟子门派 |
| `reward_disciple` | 打赏弟子 |
| `delete_disciple` | 删除弟子 |
| `add_disciple` | 添加弟子 |
| `manage_peaks` | 管理山峰（开辟新峰等） |

## 使用方法

### 引入 API 模块

```typescript
import { userApi, taskApi, sectApi } from "@/app/api/client";
```

### 示例：用户登录

```typescript
async function handleLogin(username: string, password: string) {
  try {
    const user = await userApi.login({ username, password });
    console.log("登录成功，用户:", user);
    // Token 已自动存储
  } catch (error) {
    console.error("登录失败:", error.message);
  }
}
```

### 示例：获取当前用户

```typescript
async function loadUser() {
  try {
    const user = await userApi.getCurrentUser();
    console.log("当前用户:", user);
  } catch (error) {
    console.error("获取用户信息失败:", error);
  }
}
```

### 示例：获取任务列表（带筛选）

```typescript
async function loadTasks() {
  const tasks = await taskApi.getTasks({
    difficulty: "黄金",
    status: "等待中",
    keyword: "首页",
  });
  return tasks;
}
```

### 示例：移动弟子门派

```typescript
async function moveDisciple(discipleId: string, newPeak: SectPeak) {
  const success = await sectApi.moveDisciplePeak(discipleId, newPeak);
  return success;
}
```

## 错误处理

所有 API 调用失败时会抛出 `Error` 对象，建议使用 try-catch 包裹：

```typescript
try {
  const result = await userApi.login({ username, password });
  // 成功处理
} catch (error) {
  console.error("请求失败:", error.message);
  // 错误处理（显示提示等）
}
```

### 常见错误码

| 状态码 | 说明 |
|--------|------|
| 401 | 未授权 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 迁移记录

### 架构调整

- **日期**: 2026-07-21
- **调整内容**:
  1. 所有后端 API 调用统一迁移至 `app/api/client/index.ts`
  2. 移除 `services/` 目录下的模拟服务（保留文件结构，内容已标记为废弃）
  3. 清空 `data/` 目录下的模拟数据
  4. 所有页面组件已从 `@/services/xxxService` 迁移至 `@/app/api/client`

### 新增功能

1. **JWT Token 认证机制**
   - Access Token 和 Refresh Token 自动管理
   - Token 过期检测和自动刷新
   - 认证失败自动重试
   - 退出登录时清除 Token

2. **统一 Base URL**
   - 从相对路径 `/api` 改为完整 URL `http://localhost:8080/api`

3. **认证接口标记**
   - 所有需要认证的接口已标记 `requiresAuth: true`
   - 请求时自动注入 Authorization Header

### 已迁移文件

| 原位置 | 新位置 | 状态 |
|--------|--------|------|
| `services/userService.ts` | `app/api/client/index.ts` (userApi) | 已迁移 |
| `services/taskService.ts` | `app/api/client/index.ts` (taskApi) | 已迁移 |
| `services/sectService.ts` | `app/api/client/index.ts` (sectApi) | 已迁移 |

### 已更新页面

- `components/Navbar.tsx`
- `app/page.tsx`
- `app/login/page.tsx`
- `app/register/page.tsx`
- `app/profile/page.tsx`
- `app/task-hall/page.tsx`
- `app/sect-affairs/page.tsx`

## 后端集成注意事项

1. 确保后端 API 接口规范与本文档一致
2. 登录/注册接口需返回 `{ token, refreshToken?, user }` 格式
3. Token 刷新接口路径为 `POST /user/refresh`，请求体为 `{ refreshToken }`
4. 需要认证的接口需校验 `Authorization: Bearer <token>` 请求头
5. 所有响应数据使用 JSON 格式
6. 错误响应建议包含 `message` 字段便于前端展示
