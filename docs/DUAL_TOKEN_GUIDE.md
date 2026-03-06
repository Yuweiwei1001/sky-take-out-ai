# 管理端双token机制使用指南

## 概述

管理端采用双token机制提升安全性和用户体验：

| 令牌类型 | 有效期 | 用途 |
|---------|--------|------|
| **Access Token** | 30分钟 | 日常API请求认证 |
| **Refresh Token** | 7天 | 用于刷新 Access Token |

## 登录接口变化

### 请求
```http
POST /admin/employee/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

### 响应
```json
{
  "code": 1,
  "data": {
    "id": 1,
    "userName": "admin",
    "name": "管理员",
    "token": "eyJhbGciOiJIUzI1NiIs...",      // Access Token
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...", // Refresh Token
    "expiresIn": 1800                           // Access Token 过期时间(秒)
  }
}
```

## 前端实现建议

### 1. 存储方式

```javascript
// Access Token - 内存存储（如 Redux/Vuex）
store.commit('setAccessToken', response.data.token);

// Refresh Token - localStorage 或 httpOnly Cookie
localStorage.setItem('refreshToken', response.data.refreshToken);
```

### 2. 请求拦截器

```javascript
// 每个请求自动携带 Access Token
axios.interceptors.request.use(config => {
  const token = store.state.accessToken;
  if (token) {
    config.headers['token'] = token;
  }
  return config;
});
```

### 3. 响应拦截器（自动刷新）

```javascript
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    // Access Token 过期 (401)
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 使用 Refresh Token 获取新的双token
        const refreshToken = localStorage.getItem('refreshToken');
        const res = await axios.post('/admin/token/refresh', null, {
          headers: { 'Refresh-Token': refreshToken }
        });

        // 更新存储
        store.commit('setAccessToken', res.data.data.accessToken);
        localStorage.setItem('refreshToken', res.data.data.refreshToken);

        // 重试原请求
        originalRequest.headers['token'] = res.data.data.accessToken;
        return axios(originalRequest);

      } catch (refreshError) {
        // Refresh Token 也过期了，跳转登录页
        store.commit('clearToken');
        localStorage.removeItem('refreshToken');
        router.push('/login');
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

### 4. 主动登出

```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');

  // 通知后端吊销 Refresh Token
  await axios.post('/admin/token/revoke', null, {
    headers: { 'Refresh-Token': refreshToken }
  });

  // 清除本地存储
  store.commit('clearToken');
  localStorage.removeItem('refreshToken');
  router.push('/login');
}
```

## Token 刷新接口

### 请求
```http
POST /admin/token/refresh
Refresh-Token: eyJhbGciOiJIUzI1NiIs...
```

### 响应
```json
{
  "code": 1,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 1800,
    "tokenType": "Bearer"
  }
}
```

## 验证 Refresh Token 接口

### 请求
```http
GET /admin/token/validate
Refresh-Token: eyJhbGciOiJIUzI1NiIs...
```

### 响应
```json
{
  "code": 1,
  "data": true  // true-有效, false-无效
}
```

## 状态码说明

| 状态码 | 含义 | 处理方式 |
|--------|------|---------|
| 401 | Access Token 过期或无效 | 使用 Refresh Token 刷新 |
| 403 | Refresh Token 过期或无效 | 跳转登录页 |
| 200 | 请求成功 | 正常处理 |

## 安全建议

1. **定时刷新**: 在 Access Token 过期前（如25分钟）主动刷新，避免请求中断
2. **并发控制**: 同时多个请求过期时，只发送一次刷新请求
3. **重试机制**: 刷新失败后可重试1-2次
4. **异常处理**: 网络异常时保留原token，避免误删

## 后端实现说明

### 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `JwtProperties.java` | 修改 | 添加 Refresh Token 配置 |
| `application.yml` | 修改 | 配置双token参数 |
| `EmployeeLoginVO.java` | 修改 | 添加 refreshToken 和 expiresIn |
| `TokenRefreshVO.java` | 新增 | Token刷新响应对象 |
| `TokenService.java` | 新增 | Token服务接口 |
| `TokenServiceImpl.java` | 新增 | Token服务实现（Redis存储） |
| `TokenController.java` | 新增 | Token刷新/验证/吊销接口 |
| `EmployeeController.java` | 修改 | 登录/登出逻辑更新 |
| `JwtTokenAdminInterceptor.java` | 修改 | 处理 Access Token 过期 |
| `WebMvcConfiguration.java` | 修改 | 添加刷新接口白名单 |

### Redis 存储结构

```
refresh_token:{jti}  ->  empId  (过期时间: 7天)
token_family:{empId}  ->  Set<jti>  (过期时间: 7天)
```

### 特性

- **轮换刷新**: 每次刷新都生成新的 Refresh Token，旧Token立即失效
- **多端支持**: 同一账号可在多设备登录，互不影响
- **强制下线**: 修改密码、禁用账号时可吊销所有Token
