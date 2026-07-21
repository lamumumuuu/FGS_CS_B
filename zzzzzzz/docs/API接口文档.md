# 前后端连接接口文档

## 目录

- [1. 概述](#1-概述)
- [2. 接口设计规范](#2-接口设计规范)
- [3. API 基础配置](#3-api-基础配置)
- [4. 认证方式](#4-认证方式)
- [5. 统一响应格式](#5-统一响应格式)
- [6. 错误码定义](#6-错误码定义)
- [7. API 端点列表](#7-api-端点列表)
- [8. 接口调用示例](#8-接口调用示例)
- [9. 操作步骤说明](#9-操作步骤说明)
- [10. 附录](#10-附录)

---

## 1. 概述

本文档详细描述 fgs 计算机协会官网前端与后端 API 的连接规范、调用方法和数据格式。技术人员可依据本文档独立完成前后端对接、接口调试及集成测试等工作。

### 1.1 项目背景

- **项目名称**: 计算机协会管理系统
- **后端技术栈**: Spring Boot 3.5.10 + MyBatis-Plus 3.5.13 + PostgreSQL 16
- **前端技术栈**: React/Next.js (TypeScript)
- **接口风格**: RESTful API
- **数据格式**: JSON
- **认证方式**: JWT (JSON Web Token)

### 1.2 适用范围

本文档适用于：
- 前端开发人员进行接口对接
- 后端开发人员进行接口开发与维护
- 测试人员进行接口测试
- 运维人员进行系统部署与监控

---

## 2. 接口设计规范

### 2.1 URL 命名规范

- **基础路径**: 所有 API 接口均以 `/api` 为前缀
- **资源命名**: 使用小写字母和连字符（kebab-case）
- **版本控制**: 当前版本为 v1，暂不体现在 URL 中

```
格式: /api/{resource}/{action}
示例: /api/user/login, /api/user/info
```

### 2.2 HTTP 方法规范

| 方法 | 用途 | 示例 |
|------|------|------|
| GET | 查询资源 | GET /api/user/info |
| POST | 创建资源 / 提交操作 | POST /api/user/login |
| PUT | 更新资源 | PUT /api/user/reset-password |
| DELETE | 删除资源 | DELETE /api/user/{id} |
| OPTIONS | 预检请求 | OPTIONS /api/user |

### 2.3 请求头规范

| 请求头 | 说明 | 必填 | 示例 |
|--------|------|------|------|
| Content-Type | 请求体类型 | 是 | application/json |
| Authorization | 认证令牌 | 认证接口必填 | Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... |

### 2.4 数据类型规范

| 数据类型 | 说明 | 示例 |
|----------|------|------|
| string | 字符串 | "username, "2024-01-01" |
| integer | 整数 | 1, 100, -1 |
| number | 浮点数 | 3.14, 100.00 |
| boolean | 布尔值 | true, false |
| array | 数组 | [1, 2, 3] |
| object | 对象 | {"key": "value"} |
| null | 空值 | null |

### 2.5 分页参数规范

分页查询接口统一使用以下参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | integer | 1 | 当前页码，从1开始 |
| pageSize | integer | 10 | 每页条数 |

---

## 3. API 基础配置

### 3.1 开发环境配置

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 基础 URL | `http://localhost:8080/api` | 本地开发环境 |
| 数据格式 | JSON (application/json) | 请求和响应均使用 JSON 格式 |
| 字符编码 | UTF-8 | 统一使用 UTF-8 编码 |
| 认证方式 | JWT (JSON Web Token) | Bearer Token 认证 |

### 3.2 跨域配置

后端已配置 CORS 跨域支持：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 允许的源 | http://localhost:3000, http://localhost:5173 | 前端开发服务器地址 |
| 允许的方法 | GET, POST, PUT, DELETE, OPTIONS | 支持的 HTTP 方法 |
| 允许的请求头 | Authorization, Content-Type, X-Requested-With | 允许的自定义请求头 |
| 预检缓存时间 | 3600 秒 | OPTIONS 请求缓存时间 |

---

## 4. 认证方式

### 4.1 JWT 认证机制

本系统采用 JWT (JSON Web Token) 进行用户认证，使用 Bearer Token 方式传递。

#### 4.1.1 Token 结构

JWT Token 由三部分组成：
- **Header**: 算法和令牌类型
- **Payload**: 声明（用户名、创建时间、过期时间等
- **Signature**: 签名

#### 4.1.2 Token 生成与验证

- **签名算法**: HS256 (HMAC-SHA256)
- **Token 有效期**: 默认 86400 秒（24小时）
- **密钥配置**: 通过环境变量 `JWT_SECRET` 配置

### 4.2 认证流程

```
用户登录
    |
    v
前端发送用户名/密码
    |
    v
后端验证凭据
    |
    +-- 验证失败 --> 返回错误信息
    |
    +-- 验证成功 --> 生成 JWT Token
                        |
                        v
                  返回 Token 和用户信息
                        |
                        v
                  前端存储 Token (localStorage)
                        |
                        v
后续请求携带 Authorization: Bearer <token>
                        |
                        v
后端验证 Token 有效性
                        |
                        +-- Token 有效 --> 处理请求
                        |
                        +-- Token 无效/过期 --> 返回 401
```

### 4.3 Token 存储规范

前端 Token 存储建议：

| Token 类型 | 存储位置 | 键名 | 说明 |
|----------|----------|------|------|
| Access Token | localStorage | fgscs_token | 访问令牌 |
| Refresh Token | localStorage | fgscs_refresh_token | 刷新令牌（可选） |

### 4.4 认证接口标记

需要认证的接口需在请求头中携带 Token：

```
Authorization: Bearer <token>
```

### 4.5 公开接口列表

以下接口无需认证即可访问：

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | /api/user/captcha | 获取图片验证码 |
| POST | /api/user/register | 用户注册 |
| POST | /api/user/login | 用户登录 |
| POST | /api/user/send-code | 发送注册验证码 |
| POST | /api/user/send-reset-code | 发送重置密码验证码 |
| PUT | /api/user/reset-password | 重置密码 |
| GET | /actuator/health | 健康检查 |
| GET | /swagger-ui/** | API 文档 |
| GET | /v3/api-docs/** | OpenAPI 规范 |

---

## 5. 统一响应格式

### 5.1 响应结构

所有 API 响应均使用统一的包装格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 5.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 状态码，200 表示成功，其他表示失败 |
| message | string | 响应消息描述 |
| data | object/array/null | 响应数据，失败时为 null |

### 5.3 成功响应示例

#### 5.3.1 带数据的成功响应

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com"
    }
  }
}
```

#### 5.3.2 仅消息的成功响应

```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

### 5.4 失败响应示例

```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

---

## 6. 错误码定义

### 6.1 HTTP 状态码

| 状态码 | 说明 | 场景 |
|--------|------|------|
| 200 | OK | 请求成功 |
| 400 | Bad Request | 参数校验失败、业务逻辑错误 |
| 401 | Unauthorized | 未认证、Token 无效或过期 |
| 403 | Forbidden | 无权限访问 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

### 6.2 业务错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 操作成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式和内容 |
| 401 | 令牌无效或已过期 | 重新登录获取新 Token |
| 403 | 无权限访问 | 联系管理员分配权限 |
| 404 | 资源不存在 | 检查请求的资源 ID 是否正确 |
| 500 | 服务器内部错误 | 联系后端开发人员排查 |

### 6.3 常见业务错误消息

| 错误消息 | 说明 | 触发场景 |
|----------|------|----------|
| 用户名或密码错误 | 登录凭据无效 | 登录时用户名/密码不匹配 |
| 用户名已存在 | 注册用户名重复 | 注册时用户名已被使用 |
| 邮箱已被注册 | 注册邮箱重复 | 注册时邮箱已被使用 |
| 验证码错误 | 图形验证码错误 | 注册时验证码不匹配 |
| 验证码已过期 | 验证码超时 | 验证码超过有效期（5分钟） |
| 令牌无效或已过期 | JWT 认证失败 | Token 无效或过期 |
| 用户不存在 | 查询用户信息失败 | 用户已被删除或不存在 |
| 系统繁忙，请稍后重试 | 未知系统错误 | 服务器内部异常 |

---

## 7. API 端点列表

### 7.1 用户认证模块

#### 7.1.1 获取图片验证码

- **接口地址**: `GET /api/user/captcha`
- **是否认证**: 否
- **接口描述**: 生成图形验证码，返回验证码 key 和 Base64 编码的图片数据

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| captchaKey | string | 验证码唯一标识 |
| captchaImage | string | Base64 编码的验证码图片（含 data:image/png;base64, 前缀 |

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "captchaKey": "550e8400-e29b-41d4-a716-446655440000",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

**有效期**: 5 分钟

---

#### 7.1.2 发送注册验证码

- **接口地址**: `POST /api/user/send-code`
- **是否认证**: 否
- **接口描述**: 向指定邮箱发送注册验证码

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |

**请求示例**:

```json
{
  "email": "user@example.com"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "验证码已发送至您的邮箱",
  "data": null
}
```

**错误响应**:

```json
{
  "code": 400,
  "message": "邮箱已被注册",
  "data": null
}
```

---

#### 7.1.3 用户注册

- **接口地址**: `POST /api/user/register`
- **是否认证**: 否
- **接口描述**: 使用用户名、邮箱、密码和验证码注册新用户

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，长度 2-20 个字符 |
| email | string | 是 | 邮箱地址 |
| password | string | 是 | 密码，长度 6-20 个字符 |
| captchaKey | string | 是 | 验证码 key |
| captchaCode | string | 是 | 图形验证码 |

**请求示例**:

```json
{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "password": "123456",
  "captchaKey": "550e8400-e29b-41d4-a716-446655440000",
  "captchaCode": "aB3x"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

**错误响应**:

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

---

#### 7.1.4 用户登录

- **接口地址**: `POST /api/user/login`
- **是否认证**: 否
- **接口描述**: 使用用户名/邮箱和密码登录

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名或邮箱 |
| password | string | 是 | 密码 |

**请求示例**:

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 访问令牌 |
| user | object | 用户信息对象 |

**用户信息对象 (User)**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 用户 ID |
| username | string | 用户名 |
| email | string | 邮箱 |
| avatar | string | 头像 URL |
| role | integer | 角色：0-普通用户，1-干事，2-管理员 |
| status | integer | 用户状态：0-禁用，1-启用 |
| createTime | string | 创建时间 |
| updateTime | string | 更新时间 |
| lastLoginTime | string | 最后登录时间 |

**响应示例**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InpoYW5nc2FuIiwiY3JlYXRlZCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ...",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "email": "zhangsan@example.com",
      "avatar": null,
      "role": 0,
      "status": 1,
      "createTime": "2024-01-01T00:00:00",
      "updateTime": "2024-01-01T00:00:00",
      "lastLoginTime": null
    }
  }
}
```

**错误响应**:

```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

---

#### 7.1.5 获取用户信息

- **接口地址**: `GET /api/user/info`
- **是否认证**: 是
- **接口描述**: 通过 JWT 令牌获取当前用户信息

**请求头**:

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "avatar": null,
    "role": 0,
    "status": 1,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00",
    "lastLoginTime": "2024-01-02T10:00:00"
  }
}
```

**错误响应**:

```json
{
  "code": 401,
  "message": "令牌无效或已过期",
  "data": null
}
```

---

#### 7.1.6 发送重置密码验证码

- **接口地址**: `POST /api/user/send-reset-code`
- **是否认证**: 否
- **接口描述**: 向邮箱发送重置密码验证码

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |

**请求示例**:

```json
{
  "email": "user@example.com"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "验证码已发送至您的邮箱",
  "data": null
}
```

---

#### 7.1.7 重置密码

- **接口地址**: `PUT /api/user/reset-password`
- **是否认证**: 否
- **接口描述**: 使用邮箱、新密码和验证码重置密码

**请求参数**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |
| newPassword | string | 是 | 新密码，长度 6-20 个字符 |
| verificationCode | string | 是 | 邮箱验证码 |

**请求示例**:

```json
{
  "email": "user@example.com",
  "newPassword": "654321",
  "verificationCode": "123456"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": null
}
```

**错误响应**:

```json
{
  "code": 400,
  "message": "验证码错误",
  "data": null
}
```

---

### 7.2 系统监控模块

#### 7.2.1 健康检查

- **接口地址**: `GET /actuator/health`
- **是否认证**: 否
- **接口描述**: 检查应用健康状态

**响应示例**:

```json
{
  "status": "UP"
}
```

---

### 7.3 API 文档模块

#### 7.3.1 Swagger UI

- **访问地址**: `http://localhost:8080/swagger-ui/index.html`
- **是否认证**: 否
- **接口描述**: 交互式 API 文档页面

#### 7.3.2 OpenAPI 规范

- **接口地址**: `GET /v3/api-docs`
- **是否认证**: 否
- **接口描述**: OpenAPI 3.0 规范 JSON 数据

---

## 8. 接口调用示例

### 8.1 使用 cURL 调用

#### 8.1.1 获取验证码

```bash
curl -X GET "http://localhost:8080/api/user/captcha" \
  -H "Content-Type: application/json"
```

#### 8.1.2 用户注册

```bash
curl -X POST "http://localhost:8080/api/user/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "password": "123456",
    "captchaKey": "550e8400-e29b-41d4-a716-446655440000",
    "captchaCode": "aB3x"
  }'
```

#### 8.1.3 用户登录

```bash
curl -X POST "http://localhost:8080/api/user/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "password": "123456"
  }'
```

#### 8.1.4 获取用户信息（需要认证）

```bash
curl -X GET "http://localhost:8080/api/user/info" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 8.2 使用 JavaScript Fetch 调用

#### 8.2.1 基础请求封装

```javascript
const BASE_URL = 'http://localhost:8080/api';

async function request(endpoint, options = {}, requiresAuth = false) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  };

  if (requiresAuth) {
    const token = localStorage.getItem('fgscs_token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers
  });

  const result = await response.json();

  if (result.code !== 200) {
    throw new Error(result.message);
  }

  return result.data;
}
```

#### 8.2.2 用户登录示例

```javascript
async function login(username, password) {
  try {
    const data = await request('/user/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    localStorage.setItem('fgscs_token', data.token);
    localStorage.setItem('fgscs_user', JSON.stringify(data.user));

    return data.user;
  } catch (error) {
    console.error('登录失败:', error.message);
    throw error;
  }
}
```

#### 8.2.3 获取用户信息示例

```javascript
async function getUserInfo() {
  try {
    const user = await request('/user/info', {
      method: 'GET'
    }, true);

    return user;
  } catch (error) {
    console.error('获取用户信息失败:', error.message);
    if (error.message.includes('令牌无效或已过期')) {
      localStorage.removeItem('fgscs_token');
      localStorage.removeItem('fgscs_user');
    }
    throw error;
  }
}
```

### 8.3 使用 Axios 调用

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('fgscs_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  response => {
    const result = response.data;
    if (result.code !== 200) {
      return Promise.reject(new Error(result.message));
    }
    return result.data;
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('fgscs_token');
      localStorage.removeItem('fgscs_user');
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 9. 操作步骤说明

### 9.1 环境准备步骤

#### 步骤 1: 确认后端服务运行

1. 启动后端 Spring Boot 应用
2. 验证服务是否正常运行：

```bash
curl http://localhost:8080/actuator/health
```

预期返回：
```json
{"status":"UP"}
```

#### 步骤 2: 访问 API 文档

1. 打开浏览器访问 `http://localhost:8080/swagger-ui/index.html
2. 查看所有可用接口列表
3. 可在页面上直接测试接口

### 9.2 用户注册流程

```
1. 调用 GET /api/user/captcha 获取图形验证码
2. 调用 POST /api/user/send-code 发送邮箱验证码
3. 调用 POST /api/user/register 提交注册信息
4. 注册成功后跳转登录页
```

### 9.3 用户登录流程

```
1. 用户输入用户名/邮箱和密码
2. 调用 POST /api/user/login
3. 登录成功后保存 Token 和用户信息到 localStorage
4. 后续请求自动携带 Authorization 头
```

### 9.4 密码重置流程

```
1. 用户点击"忘记密码"
2. 输入注册邮箱
3. 调用 POST /api/user/send-reset-code 发送验证码
4. 用户输入邮箱收到的验证码和新密码
5. 调用 PUT /api/user/reset-password 重置密码
6. 重置成功后跳转登录页
```

### 9.5 Token 过期处理

```
1. 发送请求时后端返回 401 错误
2. 清除本地存储的 Token 和用户信息
3. 提示用户登录已过期
4. 跳转至登录页面
5. 用户重新登录获取新 Token
```

---

## 10. 附录

### 10.1 数据字典

#### 10.1.1 用户角色 (role)

| 值 | 说明 |
|----|------|
| 0 | 普通用户 |
| 1 | 干事 |
| 2 | 管理员 |

#### 10.1.2 用户状态 (status)

| 值 | 说明 |
|----|------|
| 0 | 禁用 |
| 1 | 启用 |

### 10.2 相关文件索引

| 文件路径 | 说明 |
|----------|------|
| [UserController.java](file:///c:/Users/admin/Desktop/fgs2026-01-24-service-master/src/main/java/com/example/computerassociation/controller/UserController.java) | 用户控制器 |
| [Result.java](file:///c:/Users/admin/Desktop/fgs2026-01-24-service-master/src/main/java/com/example/computerassociation/common/Result.java) | 统一响应封装 |
| [JwtUtil.java](file:///c:/Users/admin/Desktop/fgs2026-01-24-service-master/src/main/java/com/example/computerassociation/util/JwtUtil.java) | JWT 工具类 |
| [SecurityConfig.java](file:///c:/Users/admin/Desktop/fgs2026-01-24-service-master/src/main/java/com/example/computerassociation/config/SecurityConfig.java) | 安全配置 |
| [application.yml](file:///c:/Users/admin/Desktop/fgs2026-01-24-service-master/src/main/resources/application.yml) | 应用配置 |

### 10.3 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-07-21 | 初始版本，包含用户认证模块接口 |
