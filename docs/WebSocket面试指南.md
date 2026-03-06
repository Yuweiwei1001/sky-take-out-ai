# WebSocket 面试指南

## 一、项目实现相关问题

### 1. 你是怎么实现 WebSocket 功能的？

**回答框架：**
```
技术选型 → 架构设计 → 核心实现 → 问题解决
```

**参考回答：**
> 我使用原生 WebSocket 实现了一个订单实时推送系统。后端基于 Jakarta WebSocket 标准（`@ServerEndpoint`），前端使用浏览器原生 WebSocket API。
>
> 核心功能包括：
> - 来单提醒、催单通知的消息推送
> - 心跳保活机制（30秒间隔）
> - 自动重连机制（最多5次，间隔3秒）
> - 浏览器桌面通知集成
>
> 开发中遇到的主要问题：
> - **跨域问题**：通过 Vite 代理配置解决开发环境跨域
> - **连接稳定性**：实现心跳检测和自动重连
> - **JWT 拦截**：将 `/ws/**` 路径排除在拦截器外

---

### 2. 为什么选择 WebSocket 而不是其他方案？

**对比表：**

| 方案 | 实时性 | 复杂度 | 适用场景 | 你的选择理由 |
|------|--------|--------|----------|-------------|
| **WebSocket** | 高（全双工） | 中 | 即时通讯、实时推送 | ✅ 最适合本项目 |
| **SSE (Server-Sent Events)** | 高（单向） | 低 | 股票行情、新闻推送 | 仅服务端推送，不够灵活 |
| **长轮询 (Long Polling)** | 中 | 低 | 兼容性要求高 | 资源消耗大 |
| **短轮询** | 低 | 低 | 简单场景 | 实时性差，浪费带宽 |

**关键回答点：**
- 外卖订单需要**双向通信**（不仅推送，后续可能扩展客服聊天）
- WebSocket 建立连接后**头部开销小**，适合高频消息
- 现代浏览器支持良好，无需兼容 IE

---

### 3. 如何保证连接稳定性？

**三层保障机制：**

```
┌─────────────────────────────────────────┐
│  第一层：心跳保活（应用层）               │
│  - 客户端每30秒发送 ping                  │
│  - 服务端回复 pong                        │
│  - 检测连接是否存活                       │
├─────────────────────────────────────────┤
│  第二层：自动重连（应用层）               │
│  - 连接断开时自动重连                     │
│  - 指数退避策略（3秒间隔，最多5次）        │
│  - 避免无限重连造成资源浪费               │
├─────────────────────────────────────────┤
│  第三层：异常处理（代码层）               │
│  - onerror 事件捕获                       │
│  - onclose 状态码分析                     │
│  - 清理失效会话                           │
└─────────────────────────────────────────┘
```

---

### 4. 服务端如何管理多个客户端连接？

**代码层面：**
```java
// 使用 ConcurrentHashMap 存储会话（线程安全）
private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

// 群发消息
public void sendToAllClient(String message) {
    sessionMap.values().forEach(session -> {
        session.getBasicRemote().sendText(message);
    });
}
```

**扩展问题：** 如果是分布式环境怎么办？
- 使用 Redis Pub/Sub 做消息广播
- 或使用 MQ（RabbitMQ/Kafka）转发消息到各节点

---

## 二、WebSocket 原理与特性

### 1. WebSocket 是什么？有什么特点？

**定义：**
> WebSocket 是一种在单个 TCP 连接上进行**全双工通信**的协议，位于 OSI 模型的应用层。

**核心特性（必须记住）：**

| 特性 | 说明 | 对比 HTTP |
|------|------|----------|
| **全双工** | 客户端和服务端可同时发送数据 | HTTP 半双工，一问一答 |
| **持久连接** | 建立后保持连接状态 | HTTP 无状态，每次需重新建立 |
| **低延迟** | 无需重复握手 | HTTP 每次请求都带完整头部 |
| **二进制支持** | 支持文本和二进制帧 | HTTP  mainly 文本 |

---

### 2. WebSocket 握手过程是怎样的？

**图解：**

```
客户端                              服务端
   │                                   │
   │  ① GET /chat HTTP/1.1            │
   │     Connection: Upgrade          │
   │     Upgrade: websocket           │
   │     Sec-WebSocket-Key: xxx       │
   │─────────────────────────────────>│
   │                                   │
   │  ② HTTP/1.1 101 Switching       │
   │     Upgrade: websocket           │
   │     Sec-WebSocket-Accept: yyy    │
   │<─────────────────────────────────│
   │                                   │
   │  ③ WebSocket 连接建立成功        │
   │     可以双向传输数据帧            │
   │<===============================> │
```

**关键点：**
- 使用 HTTP 协议升级（Upgrade 机制）
- `Sec-WebSocket-Key` 是 Base64 编码的随机值
- `Sec-WebSocket-Accept` = SHA1(Key + 魔法字符串) + Base64
- 101 状态码表示协议切换成功

---

### 3. WebSocket 的帧结构了解吗？

**帧格式（了解即可）：**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - -+
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

**常用 Opcode：**
- `0x1`：文本帧
- `0x2`：二进制帧
- `0x8`：连接关闭
- `0x9`：Ping
- `0xA`：Pong

---

### 4. WebSocket 和 HTTP 有什么区别？

| 维度 | WebSocket | HTTP |
|------|-----------|------|
| **协议层次** | 应用层协议 | 应用层协议 |
| **连接方式** | 长连接，一次握手多次通信 | 短连接，每次请求需重新建立 |
| **通信模式** | 全双工（同时收发） | 半双工（请求-响应） |
| **头部开销** | 首次握手有头部，后续数据帧头部极小（2-14字节） | 每次请求都带完整头部 |
| **实时性** | 毫秒级 | 秒级（取决于轮询间隔） |
| **适用场景** | 即时通讯、游戏、实时数据 | REST API、静态资源 |

---

### 5. WebSocket 如何保持连接？

**心跳机制必要性：**
> NAT 网关、防火墙、负载均衡器会主动断开长时间无数据的空闲连接。

**实现方式：**
```javascript
// 客户端：定时发送 ping
setInterval(() => {
  ws.send('ping');
}, 30000);

// 服务端：回复 pong
@OnMessage
public void onMessage(String message, Session session) {
    if ("ping".equals(message)) {
        session.getBasicRemote().sendText("pong");
    }
}
```

**协议层心跳：**
- WebSocket 协议本身支持 Ping/Pong 帧（Opcode 0x9/0xA）
- 不需要应用层自己实现，但很多框架选择应用层实现更灵活

---

## 三、进阶问题

### 1. WebSocket 连接数有没有上限？

**理论上限：**
- 端口限制：65535 个端口
- 文件描述符限制：Linux 默认 1024，可调到 65535 或更高
- 内存限制：每个连接占用内存（约几 KB 到几十 KB）

**实际优化：**
- 使用连接池
- 水平扩展（多服务器 + 负载均衡）
- 使用更轻量级的实现（如 Netty）

---

### 2. 如何实现 WebSocket 集群？

**架构图：**

```
                    ┌─────────────┐
                    │   客户端     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │  负载均衡器  │
                    │  (Sticky    │
                    │   Session)  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
      ┌────▼────┐     ┌────▼────┐     ┌────▼────┐
      │ Server  │     │ Server  │     │ Server  │
      │   A     │     │   B     │     │   C     │
      └────┬────┘     └────┬────┘     └────┬────┘
           │               │               │
           └───────────────┼───────────────┘
                           │
                    ┌──────▼──────┐
                    │    Redis    │
                    │  Pub/Sub    │
                    └─────────────┘
```

**核心思路：**
- 客户端固定连接到某台服务器（Sticky Session）
- 服务端之间通过 Redis/MQ 广播消息
- 确保每台服务器都能收到需要转发的消息

---

### 3. WebSocket 安全性怎么保证？

**安全措施：**

| 层级 | 措施 | 说明 |
|------|------|------|
| **传输层** | WSS（WebSocket Secure） | 使用 TLS/SSL 加密 |
| **认证层** | Token 验证 | 握手时在 URL 参数或 Header 中传递 Token |
| **鉴权层** | 权限校验 | 验证用户是否有权限接收某类消息 |
| **应用层** | 消息校验 | 验证消息格式，防止注入攻击 |

**Token 传递方式：**
```javascript
// 方式1：URL 参数（常用）
const ws = new WebSocket(`wss://api.example.com/ws?token=${jwtToken}`);

// 方式2：Cookie（自动携带）
// 需要设置 withCredentials
```

---

## 四、面试技巧

### 回答结构建议

**STAR 法则：**
- **S**ituation：项目背景（外卖系统需要实时通知）
- **T**ask：你的任务（实现来单提醒、催单推送）
- **A**ction：具体行动（技术选型、编码实现、问题解决）
- **R**esult：项目成果（实时性提升、用户体验改善）

### 加分项

1. **提到具体数字**：心跳30秒、重连5次、间隔3秒
2. **对比技术方案**：为什么不用 SSE/长轮询
3. **考虑边界情况**：网络抖动、断网重连、服务器重启
4. **扩展性思考**：集群部署、水平扩展方案

### 避免踩坑

❌ 不要说：
- "WebSocket 就是 HTTP 的一种"
- "WebSocket 不需要处理断线重连"
- "WebSocket 没有连接数限制"

✅ 应该说：
- "WebSocket 通过 HTTP Upgrade 建立，但协议不同"
- "生产环境必须实现心跳和重连机制"
- "连接数受限于文件描述符和内存"

---

## 五、手写代码题

### 题目：实现一个简单的 WebSocket 客户端连接管理类

**要求：**
- 支持连接、断开、发送消息
- 实现自动重连（最多3次）
- 实现心跳保活

**参考实现：**
```javascript
class WebSocketClient {
  constructor(url) {
    this.url = url;
    this.ws = null;
    this.reconnectCount = 0;
    this.maxReconnect = 3;
    this.heartbeatTimer = null;
  }

  connect() {
    this.ws = new WebSocket(this.url);
    
    this.ws.onopen = () => {
      console.log('连接成功');
      this.reconnectCount = 0;
      this.startHeartbeat();
    };
    
    this.ws.onclose = () => {
      console.log('连接关闭');
      this.reconnect();
    };
    
    this.ws.onmessage = (e) => {
      console.log('收到消息:', e.data);
    };
  }

  reconnect() {
    if (this.reconnectCount < this.maxReconnect) {
      this.reconnectCount++;
      setTimeout(() => this.connect(), 3000);
    }
  }

  startHeartbeat() {
    this.heartbeatTimer = setInterval(() => {
      this.ws?.send('ping');
    }, 30000);
  }

  send(msg) {
    this.ws?.send(msg);
  }

  close() {
    clearInterval(this.heartbeatTimer);
    this.ws?.close();
  }
}
```

---

**祝你面试顺利！**
