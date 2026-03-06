# 双Token代码实现详解（实战篇）

## 一、项目结构概览

```
sky-take-out/
├── sky-common/                          # 公共模块
│   └── src/main/java/com/sky/
│       ├── properties/JwtProperties.java    # JWT配置
│       └── utils/JwtUtil.java               # JWT工具
├── sky-pojo/                            # 实体模块
│   └── src/main/java/com/sky/vo/
│       ├── EmployeeLoginVO.java             # 登录响应
│       └── TokenRefreshVO.java              # 刷新响应
└── sky-server/                          # 服务端
    └── src/main/java/com/sky/
        ├── config/
        │   └── WebMvcConfiguration.java       # 拦截器配置
        ├── controller/
        │   ├── EmployeeController.java        # 登录接口
        │   └── TokenController.java           # Token管理
        ├── interceptor/
        │   └── JwtTokenAdminInterceptor.java  # Token校验
        └── service/
            ├── TokenService.java              # Token接口
            └── impl/TokenServiceImpl.java     # Token实现
```

---

## 二、配置层：JwtProperties

```java
@Component
@ConfigurationProperties(prefix = "sky.jwt")
@Data
public class JwtProperties {
    // 管理端 Access Token（短期）
    private String adminSecretKey;
    private long adminTtl;              // 30分钟
    private String adminTokenName;

    // 管理端 Refresh Token（长期）⭐ 新增
    private String adminRefreshSecretKey;
    private long adminRefreshTtl;       // 7天
    private String adminRefreshTokenName;

    // 用户端单Token
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;
}
```

**配置要点**：
- 两个密钥必须不同，防止Token类型伪造
- Refresh Token有效期通常是Access的10-20倍

```yaml
sky:
  jwt:
    admin-ttl: 1800000              # 30分钟
    admin-refresh-ttl: 604800000    # 7天 = 7*24*60*60*1000
```

---

## 三、服务层：TokenServiceImpl

### 3.1 核心流程图

```
┌─────────────────────────────────────────────────────┐
│                   TokenServiceImpl                  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  generateAccessToken(empId)                         │
│       │                                             │
│       ▼                                             │
│   JWT.sign({empId, tokenType: "access", jti})       │
│       │                                             │
│       ▼                                             │
│   有效期30分钟 ──────────────────► 返回给前端        │
│                                                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│  generateRefreshToken(empId)                        │
│       │                                             │
│       ▼                                             │
│   JWT.sign({empId, tokenType: "refresh", jti})      │
│       │                                             │
│       ▼                                             │
│   存储到Redis: refresh_token:{jti} -> empId         │
│       │         (过期时间7天)                        │
│       ▼                                             │
│   返回给前端 ────────────────────► 本地存储         │
│                                                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│  refreshAccessToken(refreshToken)                   │
│       │                                             │
│       ▼                                             │
│   解析JWT，获取jti和empId                           │
│       │                                             │
│       ▼                                             │
│   查Redis: refresh_token:{jti} 存在？               │
│       │                                             │
│       ├── 不存在 ──► 抛出异常（已使用或过期）        │
│       │                                             │
│       └── 存在 ────► 删除该Token（一次性使用）       │
│                      生成新的双Token                │
│                      返回给前端                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 3.2 完整代码实现

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key 前缀
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String TOKEN_FAMILY_KEY_PREFIX = "token_family:";

    /**
     * 生成 Access Token
     */
    @Override
    public String generateAccessToken(Long empId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", empId);
        claims.put("tokenType", "access");
        claims.put("jti", UUID.randomUUID().toString()); // 唯一标识

        return JwtUtil.createJWT(
            jwtProperties.getAdminSecretKey(),
            jwtProperties.getAdminTtl(),  // 30分钟
            claims
        );
    }

    /**
     * 生成 Refresh Token ⭐ 核心方法
     */
    @Override
    public String generateRefreshToken(Long empId) {
        String tokenFamily = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("empId", empId);
        claims.put("tokenType", "refresh");
        claims.put("jti", jti);
        claims.put("tokenFamily", tokenFamily);

        String refreshToken = JwtUtil.createJWT(
            jwtProperties.getAdminRefreshSecretKey(),  // 不同密钥
            jwtProperties.getAdminRefreshTtl(),         // 7天
            claims
        );

        // ⭐ 存储到Redis，这是双Token的关键
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(
            redisKey,
            empId,
            jwtProperties.getAdminRefreshTtl(),
            TimeUnit.MILLISECONDS
        );

        // 记录token family，用于批量吊销
        String familyKey = TOKEN_FAMILY_KEY_PREFIX + empId;
        redisTemplate.opsForSet().add(familyKey, jti);
        redisTemplate.expire(familyKey, jwtProperties.getAdminRefreshTtl(), TimeUnit.MILLISECONDS);

        log.info("生成 Refresh Token，员工ID：{}，Token ID：{}", empId, jti);
        return refreshToken;
    }

    /**
     * 刷新Token ⭐ 核心方法
     */
    @Override
    public TokenRefreshVO refreshAccessToken(String refreshToken) {
        try {
            // 1. 解析 Refresh Token
            Claims claims = JwtUtil.parseJWT(
                jwtProperties.getAdminRefreshSecretKey(),
                refreshToken
            );

            // 2. 验证类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new RuntimeException("无效的令牌类型");
            }

            String jti = claims.get("jti", String.class);
            Long empId = Long.valueOf(claims.get("empId").toString());

            // 3. ⭐ 检查Redis中是否存在（防止重复使用）
            String redisKey = REFRESH_TOKEN_KEY_PREFIX + jti;
            Object cachedEmpId = redisTemplate.opsForValue().get(redisKey);

            if (cachedEmpId == null) {
                log.warn("Refresh Token 已被使用或已过期");
                throw new RuntimeException("Refresh Token 已失效，请重新登录");
            }

            // 4. ⭐ 立即删除旧的Refresh Token（轮换机制）
            redisTemplate.delete(redisKey);

            // 5. 生成新的双Token
            String newAccessToken = generateAccessToken(empId);
            String newRefreshToken = generateRefreshToken(empId);

            return TokenRefreshVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAdminTtl() / 1000)
                .tokenType("Bearer")
                .build();

        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            throw new RuntimeException("刷新 Token 失败: " + e.getMessage());
        }
    }

    /**
     * 吊销用户所有Token（强制下线）
     */
    @Override
    public void revokeRefreshToken(Long empId) {
        String familyKey = TOKEN_FAMILY_KEY_PREFIX + empId;
        var jtis = redisTemplate.opsForSet().members(familyKey);

        if (jtis != null) {
            for (Object jti : jtis) {
                redisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + jti);
            }
            redisTemplate.delete(familyKey);
        }
    }
}
```

---

## 四、控制器层

### 4.1 登录接口（EmployeeController）

```java
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO dto) {
    Employee employee = employeeService.login(dto);

    // ⭐ 生成双Token
    String accessToken = tokenService.generateAccessToken(employee.getId());
    String refreshToken = tokenService.generateRefreshToken(employee.getId());

    return Result.success(EmployeeLoginVO.builder()
        .id(employee.getId())
        .userName(employee.getUsername())
        .name(employee.getName())
        .token(accessToken)           // Access Token
        .refreshToken(refreshToken)   // Refresh Token ⭐
        .expiresIn(1800)              // 30分钟（秒）
        .build()
    );
}
```

### 4.2 Token刷新接口（TokenController）

```java
@RestController
@RequestMapping("/admin/token")
public class TokenController {

    private final TokenService tokenService;

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenRefreshVO> refreshToken(
            @RequestHeader("Refresh-Token") String refreshToken) {

        TokenRefreshVO vo = tokenService.refreshAccessToken(refreshToken);
        return Result.success(vo);
    }
}
```

---

## 五、拦截器层：JwtTokenAdminInterceptor

```java
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler) throws Exception {

        // 1. 获取Access Token
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        try {
            // 2. 解析Token
            Claims claims = JwtUtil.parseJWT(
                jwtProperties.getAdminSecretKey(),
                token
            );

            // 3. 验证Token类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"access".equals(tokenType)) {
                response.setStatus(401);
                return false;
            }

            // 4. 设置当前用户ID
            Long empId = Long.valueOf(claims.get("empId").toString());
            BaseContext.setCurrentId(empId);
            return true;

        } catch (ExpiredJwtException ex) {
            // ⭐ Access Token过期，返回401 + 特殊头
            log.info("Access Token 已过期，需要刷新");
            response.setStatus(401);
            response.setHeader("Token-Expired", "true");  // 前端识别这个头
            return false;

        } catch (Exception ex) {
            // 其他错误（伪造、格式错误等）
            response.setStatus(401);
            return false;
        }
    }
}
```

---

## 六、前端实现：request.ts

```typescript
import axios from 'axios'
import { refreshToken } from './employee'

// 是否正在刷新
let isRefreshing = false
// 等待队列
let subscribers: Array<(token: string) => void> = []

const request = axios.create({
  baseURL: '/api',
  timeout: 300000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

// 响应拦截器 ⭐ 核心逻辑
request.interceptors.response.use(
  response => response.data,
  async error => {
    const { response, config: originalRequest } = error

    // Access Token 过期
    if (response?.status === 401 && !originalRequest._retry) {
      const isTokenExpired = response.headers['token-expired'] === 'true'

      if (!isTokenExpired) {
        // 不是过期，直接跳转登录
        logout()
        return Promise.reject(error)
      }

      originalRequest._retry = true

      // ⭐ 并发控制：如果正在刷新，加入等待队列
      if (isRefreshing) {
        return new Promise(resolve => {
          subscribers.push(newToken => {
            originalRequest.headers.token = newToken
            resolve(request(originalRequest))
          })
        })
      }

      isRefreshing = true

      try {
        const storedRefreshToken = localStorage.getItem('refreshToken')

        // 调用刷新接口
        const res = await refreshToken(storedRefreshToken)

        // 更新本地存储
        localStorage.setItem('token', res.accessToken)
        localStorage.setItem('refreshToken', res.refreshToken)

        // ⭐ 通知队列中的请求
        subscribers.forEach(cb => cb(res.accessToken))
        subscribers = []

        // 重试原请求
        originalRequest.headers.token = res.accessToken
        return request(originalRequest)

      } catch (refreshError) {
        // Refresh Token 也失效，跳转登录
        logout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  window.location.href = '/login'
}

export default request
```

---

## 七、Redis存储结构

```
# Refresh Token 存储
refresh_token:{jti} -> empId (TTL: 7天)

示例：
refresh_token:550e8400-e29b-41d4-a716-446655440000 -> 1

# Token Family（用于批量吊销）
token_family:{empId} -> Set<jti> (TTL: 7天)

示例：
token_family:1 -> [550e8400-..., 6ba7b810-..., ...]
```

---

## 八、关键面试点

### 8.1 为什么Refresh Token要存Redis？

```
1. 可以设置过期时间（TTL）
2. 可以主动删除（强制下线）
3. 可以验证Token是否存在（防止重复使用）
```

### 8.2 Token轮换的好处？

```
每次刷新都生成新的Refresh Token，旧的立即失效
好处：即使Refresh Token被盗，也只能使用一次
```

### 8.3 并发请求怎么处理？

```
使用标志位isRefreshing + 等待队列subscribers
确保同一时刻只发送一次刷新请求
```
