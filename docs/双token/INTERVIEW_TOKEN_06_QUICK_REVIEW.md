# 双Token面试速查手册（快速复习篇）

## 一、5分钟快速回顾

### 核心概念
```
Access Token  = 身份证（30分钟有效，日常用）
Refresh Token = 户口本（7天有效，补身份证用）
```

### 为什么用双Token
```
单Token的问题：
- 短了：总重新登录，体验差
- 长了：被盗风险大

双Token解决：
- Access短：安全
- Refresh长：体验好
```

---

## 二、关键数字（必背）

| 参数 | 典型值 | 说明 |
|------|--------|------|
| Access Token TTL | 30分钟 | 15-60分钟都合理 |
| Refresh Token TTL | 7天 | 1-30天都合理 |
| Redis过期时间 | 与TTL相同 | 自动清理 |
| 并发等待队列 | 数组/链表 | 防止重复刷新 |

---

## 三、核心代码片段

### 3.1 生成双Token（后端）
```java
public LoginVO login(User user) {
    String accessToken = JWT.create()
        .withClaim("userId", user.getId())
        .withClaim("type", "access")
        .withExpiresAt(new Date(System.currentTimeMillis() + 30*60*1000))
        .sign(algorithm);

    String refreshToken = JWT.create()
        .withClaim("userId", user.getId())
        .withClaim("type", "refresh")
        .withClaim("jti", UUID.randomUUID())
        .withExpiresAt(new Date(System.currentTimeMillis() + 7*24*60*60*1000))
        .sign(algorithm);

    // Refresh Token存Redis
    redisTemplate.opsForValue().set(
        "refresh_token:" + jti,
        user.getId(),
        7, TimeUnit.DAYS
    );

    return new LoginVO(accessToken, refreshToken);
}
```

### 3.2 Token刷新（后端）
```java
public TokenPair refresh(String refreshToken) {
    // 1. 解析验证
    DecodedJWT jwt = JWT.verify(refreshToken);
    String jti = jwt.getClaim("jti").asString();

    // 2. 查Redis
    if (redisTemplate.get("refresh_token:" + jti) == null) {
        throw new Exception("Token已失效");
    }

    // 3. 删除旧Token（一次性使用）
    redisTemplate.delete("refresh_token:" + jti);

    // 4. 生成新双Token
    return generateNewTokenPair();
}
```

### 3.3 自动刷新（前端）
```typescript
axios.interceptors.response.use(
  res => res,
  async err => {
    if (err.response?.status === 401) {
      const res = await axios.post('/refresh', null, {
        headers: { 'Refresh-Token': localStorage.getItem('refreshToken') }
      });
      localStorage.setItem('token', res.accessToken);
      localStorage.setItem('refreshToken', res.refreshToken);
      return axios(err.config);
    }
  }
);
```

---

## 四、面试问答模板

### Q: 介绍一下双Token机制
**30秒版本**：
> "双Token使用Access Token和Refresh Token。Access Token短期有效用于API认证，Refresh Token长期有效用于续期。这样既保证了安全，又避免了频繁登录。"

**2分钟版本**：
> "双Token是为了解决单Token的安全和体验矛盾。Access Token设置30分钟过期，即使被盗影响有限；Refresh Token7天过期，存储在Redis中可以吊销。前端在Access过期时用Refresh换取新的双Token，对用户无感知。我们还做了Token轮换，每次刷新都使旧Token失效，防止重放攻击。"

### Q: 为什么管理端用双Token，用户端不用？
> "管理端是长时间操作，会话中断影响业务；用户端是短时操作，重新登录成本低。而且管理端用户少可控，用户端用户多不可控。"

### Q: Refresh Token存Redis有什么好处？
> "三个好处：1. 设置过期时间自动清理；2. 可以验证Token是否存在，防止重复使用；3. 可以主动删除实现强制下线。"

### Q: 并发请求Token过期怎么处理？
> "使用标志位isRefreshing控制只有一个请求去刷新，其他请求加入等待队列。刷新成功后统一使用新Token重试。"

---

## 五、易错点提醒

| 错误 | 正确 |
|------|------|
| Access和Refresh用同一个密钥 | 必须用不同密钥，防止伪造 |
| Refresh Token永久有效 | 必须设置过期时间，防止长期风险 |
| Refresh Token重复使用 | 每次刷新都生成新的，旧的删除 |
| 只刷新Access Token | 必须同时刷新Refresh Token |
| 前端存Refresh Token在内存 | 应该存localStorage/Cookie |

---

## 六、画图技巧

面试时画这张图，加分：

```
┌─────────────────────────────────────┐
│  用户登录                            │
│     │                               │
│     ▼                               │
│  Access  ──────▶ API请求（30分钟）   │
│  Token          过期返回401          │
│     │               │               │
│     │               ▼               │
│     │         用Refresh Token刷新    │
│     │               │               │
│     │               ▼               │
│     │         新的双Token            │
│     │               │               │
│     └───────────────┘               │
│                                     │
│  Refresh ──────▶ Redis存储（7天）    │
│  Token          可验证/可吊销        │
└─────────────────────────────────────┘
```

---

## 七、项目亮点总结

如果你面试苍穹外卖项目，这样说：

> "在这个项目中，我负责了管理端的双Token认证改造。主要实现了：
> 1. Token服务层：生成、验证、刷新、吊销的完整生命周期
> 2. Token轮换机制：每次刷新都使旧Token失效，防止重放攻击
> 3. 并发控制：使用等待队列防止重复刷新请求
> 4. 安全设计：双密钥、Redis存储、异常检测
> 5. 前端无感知：axios拦截器自动处理刷新和重试
>
> 这个方案让管理员可以连续工作7天无需登录，同时保证了安全性。"

---

## 八、一句话速记

| 概念 | 一句话 |
|------|--------|
| 双Token | Access访问，Refresh续期 |
| Token轮换 | 每次刷新都换新，旧的不让用 |
| 存Redis | 能过期、能验证、能吊销 |
| 并发控制 | 单刷新 + 队列等 |
| 管理端vs用户端 | 长操作用双，短操作用单 |

---

## 九、扩展阅读索引

- 概念原理：INTERVIEW_TOKEN_01_CONCEPT.md
- 设计决策：INTERVIEW_TOKEN_02_DESIGN_DECISION.md
- 代码实现：INTERVIEW_TOKEN_03_IMPLEMENTATION.md
- 面试题库：INTERVIEW_TOKEN_04_QA.md
- 方案对比：INTERVIEW_TOKEN_05_COMPARISON.md

---

**祝你面试成功！** 🎉
