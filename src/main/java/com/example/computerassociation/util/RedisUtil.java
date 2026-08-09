// src/main/java/com/example/computerassociation/util/RedisUtil.java

/**
 * Redis 工具类
 * 
 * 对 RedisTemplate 进行二次封装，提供常用的缓存读写方法。
 * 同时包含登录失败锁定功能：连续失败 5 次锁定 15 分钟。
 */

package com.example.computerassociation.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 登录失败计数器相关常量
    private static final String LOGIN_FAIL_PREFIX = "login_fail:";    /// Redis Key 前缀
    private static final int MAX_LOGIN_ATTEMPTS = 5;                 /// 最大尝试次数
    private static final long LOGIN_LOCK_DURATION = 15;              /// 锁定时长（分钟）

    /* ------------------------------------------------------------------ */
    /*  基础操作                                                         */
    /* ------------------------------------------------------------------ */

    /** 设置过期时间（秒） */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("设置缓存过期时间失败: key={}", key, e);
            return false;
        }
    }

    /** 获取剩余过期时间（秒） */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /** 判断 key 是否存在 */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查key是否存在失败: key={}", key, e);
            return false;
        }
    }

    /** 删除一个或多个 key */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /** 获取缓存（返回 Object） */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /** 获取缓存并转为字符串 */
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    /** 写入缓存（不设过期） */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("缓存写入失败: key={}", key, e);
            return false;
        }
    }

    /** 写入缓存（设过期时间，秒） */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("缓存写入失败: key={}, time={}", key, time, e);
            return false;
        }
    }

    /** 写入缓存（设过期时间，自定义时间单位） */
    public boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("缓存写入失败: key={}, time={}, unit={}", key, time, timeUnit, e);
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  计数器操作                                                        */
    /* ------------------------------------------------------------------ */

    /** 自增 1 */
    public long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自增失败: key={}", key, e);
            return 0;
        }
    }

    /** 自增指定值 */
    public long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自增失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }

    /** 自减 1 */
    public long decrement(String key) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自减失败: key={}", key, e);
            return 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  登录失败锁定                                                      */
    /* ------------------------------------------------------------------ */

    /** 检查用户是否已被登录锁定 */
    public boolean isLoginLocked(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        String countStr = getString(key);
        if (countStr == null) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        return count >= MAX_LOGIN_ATTEMPTS;        // 失败次数达到 5 次即锁定
    }

    /** 获取登录锁定的剩余时间（秒） */
    public long getLoginLockRemainingTime(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        return getExpire(key);
    }

    /** 记录一次登录失败（自增计数器，首次时设 15 分钟过期） */
    public void recordLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        long count = increment(key);
        if (count == 1) {
            expire(key, LOGIN_LOCK_DURATION * 60); // 首次失败开始计时
        }
        log.warn("登录失败记录: username={}, 次数={}/{}", username, count, MAX_LOGIN_ATTEMPTS);
    }

    /** 登录成功后清除失败记录 */
    public void clearLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        del(key);
    }
}