package com.example.computerassociation;

import com.example.computerassociation.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class ComputerAssociationApplicationTests {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedisConnection() {
        // 测试Redis连接
        String testKey = "test_connection_" + System.currentTimeMillis();
        String testValue = "test_value";

        // 使用RedisUtil存储值
        boolean setResult = redisUtil.set(testKey, testValue, 60); // 60秒过期
        System.out.println("Set result: " + setResult);

        // 读取值
        Object getValue = redisUtil.get(testKey);
        System.out.println("Get result: " + getValue);

        // 验证存储和读取是否成功
        assert setResult : "Redis set operation should succeed";
        assert testValue.equals(getValue) : "Retrieved value should match stored value";

        System.out.println("Redis connection test passed!");
    }

    @Test
    void testVerificationCodeStorage() {
        // 测试验证码存储
        String email = "test@example.com";
        String code = "123456";
        String key = "verification_code:" + email;

        // 存储验证码，5分钟过期
        boolean setResult = redisUtil.setVerificationCode(key, code, 5, TimeUnit.MINUTES);
        System.out.println("Verification code set result: " + setResult);

        // 读取验证码
        String getCode = redisUtil.getString(key);
        System.out.println("Verification code get result: " + getCode);

        // 验证存储和读取是否成功
        assert setResult : "Verification code storage should succeed";
        assert code.equals(getCode) : "Retrieved code should match stored code";

        System.out.println("Verification code test passed!");
    }
}