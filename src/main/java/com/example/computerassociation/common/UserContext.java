// src/main/java/com/example/computerassociation/common/UserContext.java

/**
 * 用户上下文工具类
 * 
 * 基于 ThreadLocal 存储当前请求的用户信息，确保线程安全。
 * 在请求进入时由 JWT 过滤器设置，请求结束时由过滤器清理。
 * 提供静态方法快速获取当前用户 ID、用户名、IP 等。
 */

package com.example.computerassociation.common;

import com.example.computerassociation.entity.User;

public class UserContext {

    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();           /// 当前用户完整信息
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();         /// 用户 ID
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();     /// 用户名
    private static final ThreadLocal<String> currentIp = new ThreadLocal<>();           /// 请求来源 IP

    /** 设置当前线程的用户信息，同时提取 ID 和用户名存入独立 ThreadLocal */
    public static void setUser(User user) {
        currentUser.set(user);
        if (user != null) {
            currentUserId.set(user.getId());
            currentUsername.set(user.getUsername());
        }
    }

    /** 获取当前请求的完整用户对象 */
    public static User getUser() {
        return currentUser.get();
    }

    /** 获取当前用户 ID */
    public static Long getUserId() {
        return currentUserId.get();
    }

    /** 获取当前用户名 */
    public static String getUsername() {
        return currentUsername.get();
    }

    /** 设置当前请求的客户端 IP（由过滤器调用） */
    public static void setIp(String ip) {
        currentIp.set(ip);
    }

    /** 获取当前请求的客户端 IP */
    public static String getIp() {
        return currentIp.get();
    }

    /** 清除所有 ThreadLocal，防止内存泄漏（请求结束时必须调用） */
    public static void clear() {
        currentUser.remove();
        currentUserId.remove();
        currentUsername.remove();
        currentIp.remove();
    }
}