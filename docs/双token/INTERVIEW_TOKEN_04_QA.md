# 双Token常见面试题（面试题篇）

## 一、基础概念题

### Q1: 什么是双Token机制？为什么要用双Token？

**参考答案**：

> 双Token机制是指使用 **Access Token** 和 **Refresh Token** 两个令牌来管理用户认证。
>
> **Access Token**：短期有效（15-30分钟），用于日常API请求认证。
> **Refresh Token**：长期有效（7-30天），用于在Access Token过期后获取新的Token。
>
> **使用原因**：
> 1. **安全性**：Access Token有效期短，即使被盗，攻击窗口有限
> 2. **用户体验**：Refresh Token长期有效，用户无需频繁重新登录
> 3. **可控性**：Refresh Token存储在服务端（Redis），可以主动吊销

---

### Q2: Access Token 和 Refresh Token 有什么区别？

| 维度 | Access Token | Refresh Token |
|------|-------------|---------------|
| **有效期** | 短（15-30分钟） | 长（7-30天） |
| **用途** | 访问API资源 | 获取新的Access Token |
| **存储位置** | 客户端内存 | Redis + 客户端 |
| **验证方式** | JWT签名验证 | JWT签名 + Redis存在性验证 |
| **泄露风险** | 较低（有效期短） | 较低（使用次数少+可吊销） |
| **能否吊销** | 不能（只能等过期） | 能（删除Redis即可） |

---

### Q3: 为什么 Refresh Token 要存 Redis？

**参考答案**：

> 1. **过期控制**：Redis支持TTL，可以自动过期
> 2. **存在性验证**：可以检查Token是否有效（防止重复使用）
> 3. **主动吊销**：发现异常时，可以立即删除Redis中的Token，强制用户重新登录
> 4. **多设备管理**：可以查看一个用户有哪些设备在登录

```java
// 存储到Redis
redisTemplate.opsForValue().set(
    "refresh_token:" + jti,
    empId,
    7, TimeUnit.DAYS  // TTL
);

// 验证时
if (redisTemplate.opsForValue().get(key) == null) {
    throw new Exception("Token已失效");
}
```

---

## 二、设计决策题

### Q4: 为什么管理端用双Token，用户端用单Token？

**参考答案**：

> **管理端特点**：
> - 使用时间长（几小时连续操作）
> - 安全要求高（涉及资金、数据）
> - 会话中断影响大（正在处理订单时被迫退出）
> - 需要服务端可控（发现异常可以踢人下线）
>
> **用户端特点**：
> - 使用时间短（几分钟完成下单）
> - 重新登录成本低（微信一键登录）
> - 多端登录常见（手机、平板、网页）
> - 存储成本高（百万级用户）

---

### Q5: 双Token的有效期怎么定？依据是什么？

**参考答案**：

> **Access Token（30分钟）**：
> - 足够完成一次连续操作（如处理订单、填写表单）
> - 即使被盗，攻击窗口只有30分钟
> - 太短会导致频繁刷新，影响性能
>
> **Refresh Token（7天）**：
> - 覆盖一个工作周，周末可能需要查看数据
> - 7天是一个合理的重新登录周期
> - 太长会增加被盗风险

---

### Q6: 如果让你设计一个银行系统，双Token参数怎么设？

**参考答案**：

```
银行系统安全要求极高：

Access Token：5-10分钟
- 银行操作敏感，必须尽快过期
- 每次转账确认都要验证Token有效性

Refresh Token：1天或每次使用后刷新
- 每天登录一次可以接受
- 或者每次使用Refresh Token都生成新的，且有效期1小时

额外措施：
- 绑定设备指纹
- 异常登录验证短信
- 敏感操作二次验证
```

---

## 三、安全相关题

### Q7: 如果 Refresh Token 被盗了怎么办？

**参考答案**：

> **检测手段**：
> 1. 监控同一Refresh Token的重复使用（Redis中已删除但被再次使用）
> 2. 检测异常IP、异常时间登录
> 3. 绑定设备信息，不同设备使用同一Token报警
>
> **应急处理**：
> 1. 删除Redis中的Refresh Token
> 2. 通知用户重新登录
> 3. 记录安全事件，分析泄露原因

---

### Q8: 双Token机制和Session+Cookie相比有什么优缺点？

| 维度 | 双Token（JWT） | Session+Cookie |
|------|---------------|----------------|
| **服务端存储** | Refresh Token存Redis，Access Token无状态 | Session全部存服务端 |
| **扩展性** | 好（无状态） | 差（需要共享Session） |
| **跨域支持** | 好（Header传输） | 复杂（需要处理Cookie） |
| **移动端支持** | 好 | 一般 |
| **实时吊销** | Refresh Token可吊销，Access Token不可 | Session可立即删除 |
| **性能** | 高（无需查询数据库） | 中（需要查询Session） |

---

### Q9: 如何防止 Refresh Token 被重复使用？

**参考答案**：

> **Token轮换机制（Rotation）**：
> ```
> 首次：Access A + Refresh A
> 刷新后：Access B + Refresh B（A全部失效）
> 再次刷新：Access C + Refresh C（B全部失效）
> ```
>
> **实现方式**：
> 1. 每次刷新都生成新的Refresh Token
> 2. 旧的Refresh Token从Redis中删除
> 3. 如果收到已删除的Token，说明被盗用，立即吊销该用户所有Token

---

## 四、代码实现题

### Q10: 并发请求时Access Token过期了，怎么处理？

**参考答案**：

```typescript
let isRefreshing = false;      // 是否正在刷新
let subscribers = [];          // 等待队列

// 请求拦截器
axios.interceptors.response.use(
  response => response,
  async error => {
    const { config, response } = error;

    if (response.status === 401 && !config._retry) {
      config._retry = true;

      // 如果正在刷新，加入等待队列
      if (isRefreshing) {
        return new Promise(resolve => {
          subscribers.push(newToken => {
            config.headers.token = newToken;
            resolve(axios(config));
          });
        });
      }

      isRefreshing = true;

      try {
        const res = await refreshToken();
        const newToken = res.accessToken;

        // 通知所有等待的请求
        subscribers.forEach(cb => cb(newToken));
        subscribers = [];

        return axios(config);
      } finally {
        isRefreshing = false;
      }
    }
  }
);
```

**关键点**：
- 使用 `isRefreshing` 标志位防止重复刷新
- 使用 `subscribers` 队列缓存等待的请求
- 刷新成功后统一重试

---

### Q11: 如何实现"强制用户下线"功能？

**参考答案**：

```java
@Service
public class TokenServiceImpl {

    // 吊销用户所有Refresh Token
    public void revokeRefreshToken(Long empId) {
        String familyKey = "token_family:" + empId;

        // 获取该用户的所有Token ID
        Set<Object> jtis = redisTemplate.opsForSet().members(familyKey);

        // 逐个删除
        for (Object jti : jtis) {
            redisTemplate.delete("refresh_token:" + jti);
        }

        // 删除family记录
        redisTemplate.delete(familyKey);
    }
}

// 管理员强制下线员工
@RestController
public class AdminController {

    @PostMapping("/admin/employee/force-logout/{empId}")
    public Result forceLogout(@PathVariable Long empId) {
        tokenService.revokeRefreshToken(empId);
        return Result.success();
    }
}
```

---

### Q12: 双Token项目中，如何实现"修改密码后所有设备退出"？

**参考答案**：

```java
@Service
public class EmployeeService {

    @Autowired
    private TokenService tokenService;

    @Transactional
    public void updatePassword(Long empId, String newPassword) {
        // 1. 更新密码
        employeeMapper.updatePassword(empId, encode(newPassword));

        // 2. 吊销所有Token
        tokenService.revokeRefreshToken(empId);

        // 3. （可选）记录安全日志
        securityLogService.logPasswordChange(empId);
    }
}
```

**原理**：修改密码后立即删除Redis中的所有Refresh Token，用户的Access Token过期后无法刷新，被迫重新登录。

---

## 五、场景设计题

### Q13: 用户反映"总是自动退出"，可能是什么原因？怎么排查？

**排查思路**：

```
1. 检查Refresh Token有效期
   - 查看Redis中Token是否被提前删除
   - 检查是否有定期清理任务误删

2. 检查并发刷新问题
   - 多个请求同时过期，是否有竞态条件
   - 队列机制是否正常工作

3. 检查多端登录
   - 用户是否在手机、电脑同时登录
   - Token轮换是否导致一端失效

4. 检查网络问题
   - 刷新接口是否超时
   - 网络不稳定导致刷新失败

5. 检查Redis连接
   - Redis是否偶尔断开
   - 集群模式下数据同步是否延迟
```

---

### Q14: 如果要支持"单设备登录"（新登录踢掉旧登录），怎么改？

**参考答案**：

```java
@Service
public class TokenServiceImpl {

    public String generateRefreshToken(Long empId) {
        // 1. 生成新Token前，先吊销旧Token
        revokeRefreshToken(empId);

        // 2. 生成新的Token
        String jti = UUID.randomUUID().toString();
        // ... 存储到Redis

        return refreshToken;
    }
}
```

**原理**：每次生成新Token前，先删除该用户的所有旧Token，保证只有一个设备有效。

---

## 六、高级话题

### Q15: 双Token在微服务架构中怎么传递？

**参考答案**：

```
Gateway（网关层）
    │
    ├── 验证Access Token
    │
    ├── 将用户ID添加到Header
    │   X-User-Id: 123
    │   X-User-Role: ADMIN
    │
    └── 转发到微服务

微服务
    │
    └── 从Header获取用户信息，无需再次验证Token

注意：
- 微服务之间调用也带这个Header
- 内部网络可信，不需要再验证JWT
- Gateway到微服务使用mTLS保证安全
```

---

### Q16: 如何实现"无感知续期"？

**参考答案**：

```javascript
// 定时器方式：在Token过期前主动刷新
class TokenManager {
  constructor() {
    this.refreshTimer = null;
  }

  startRefreshTimer(expiresIn) {
    // 在过期前5分钟刷新
    const refreshTime = (expiresIn - 300) * 1000;

    this.refreshTimer = setTimeout(() => {
      this.refresh();
    }, refreshTime);
  }

  async refresh() {
    try {
      const res = await refreshToken();
      localStorage.setItem('token', res.accessToken);
      localStorage.setItem('refreshToken', res.refreshToken);

      // 继续下一轮定时
      this.startRefreshTimer(res.expiresIn);
    } catch (error) {
      // 刷新失败，跳转登录
      window.location.href = '/login';
    }
  }
}
```

**优点**：用户完全无感知，不会在操作过程中突然过期。

---

## 七、一句话总结

| 问题 | 一句话回答 |
|------|-----------|
| 什么是双Token | Access Token访问，Refresh Token续期 |
| 为什么用双Token | 安全与体验的平衡 |
| 为什么存Redis | 可过期、可验证、可吊销 |
| Token轮换 | 每次刷新都换新，旧的作废 |
| 并发处理 | 单刷新请求 + 等待队列 |
| 强制下线 | 删Redis，让Token无法刷新 |

---

## 八、面试技巧

1. **画图**：手绘双Token流程图，展示专业度
2. **说数字**："Access Token 30分钟，Refresh Token 7天"
3. **讲场景**：用"餐厅老板处理订单"的场景说明痛点
4. **谈权衡**：展示对安全性、体验、成本的综合考量
5. **聊扩展**：提到微服务、单设备登录等进阶话题
