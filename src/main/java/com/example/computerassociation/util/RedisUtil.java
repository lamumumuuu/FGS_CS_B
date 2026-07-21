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

    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_DURATION = 15;

    private static final String CODE_SEND_PREFIX = "code_send:";
    private static final long CODE_SEND_INTERVAL = 60;

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

    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查key是否存在失败: key={}", key, e);
            return false;
        }
    }

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

    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("缓存写入失败: key={}", key, e);
            return false;
        }
    }

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

    public long increment(String key) {
        try {
            Long result = redisTemplate.opsForValue().increment(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自增失败: key={}", key, e);
            return 0;
        }
    }

    public long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自增失败: key={}, delta={}", key, delta, e);
            return 0;
        }
    }

    public long decrement(String key) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("计数器自减失败: key={}", key, e);
            return 0;
        }
    }

    public boolean setVerificationCode(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
                Object retrievedValue = redisTemplate.opsForValue().get(key);
                if (retrievedValue != null) {
                    return true;
                } else {
                    log.error("验证码存储后验证失败: key={}", key);
                    return false;
                }
            } else {
                return set(key, value);
            }
        } catch (Exception e) {
            log.error("验证码存储失败: key={}", key, e);
            return false;
        }
    }

    public boolean isLoginLocked(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        String countStr = getString(key);
        if (countStr == null) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        return count >= MAX_LOGIN_ATTEMPTS;
    }

    public long getLoginLockRemainingTime(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        return getExpire(key);
    }

    public void recordLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        long count = increment(key);
        if (count == 1) {
            expire(key, LOGIN_LOCK_DURATION * 60);
        }
        log.warn("登录失败记录: username={}, 次数={}/{}", username, count, MAX_LOGIN_ATTEMPTS);
    }

    public void clearLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        del(key);
    }

    public boolean canSendCode(String email) {
        String key = CODE_SEND_PREFIX + email;
        return !hasKey(key);
    }

    public long getCodeSendRemainingTime(String email) {
        String key = CODE_SEND_PREFIX + email;
        return getExpire(key);
    }

    public void recordCodeSent(String email) {
        String key = CODE_SEND_PREFIX + email;
        set(key, "1", CODE_SEND_INTERVAL);
    }
}
