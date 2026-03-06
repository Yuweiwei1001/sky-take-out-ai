package com.sky.websocket;

import org.springframework.stereotype.Component;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {

    //存放会话对象 - 使用ConcurrentHashMap保证线程安全
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    //会话锁映射 - 用于保证每个会话的发送操作串行化
    private static Map<String, ReentrantLock> sessionLockMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        System.out.println("客户端：" + sid + "建立连接");
        sessionMap.put(sid, session);
        //为每个会话创建一个锁
        sessionLockMap.put(sid, new ReentrantLock());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自客户端：" + sid + "的信息:" + message);
        // 处理心跳消息
        if ("ping".equals(message)) {
            sendMessageToSession(sid, "pong");
        }
    }

    /**
     * 向指定会话发送消息（线程安全）
     *
     * @param sid     会话ID
     * @param message 消息内容
     */
    private void sendMessageToSession(String sid, String message) {
        Session session = sessionMap.get(sid);
        if (session == null || !session.isOpen()) {
            return;
        }

        ReentrantLock lock = sessionLockMap.get(sid);
        if (lock == null) {
            return;
        }

        lock.lock();
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            System.err.println("发送消息失败 [sid=" + sid + "]: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("连接断开:" + sid);
        sessionMap.remove(sid);
        sessionLockMap.remove(sid);
    }

    /**
     * 群发消息（线程安全）
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            sendMessageToSession(entry.getKey(), message);
        }
    }

}
