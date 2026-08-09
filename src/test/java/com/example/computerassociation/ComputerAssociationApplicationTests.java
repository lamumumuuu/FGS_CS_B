package com.example.computerassociation;

import com.example.computerassociation.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

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
        String testKey = "test_connection_" + System.currentTimeMillis();
        String testValue = "test_value";

        boolean setResult = redisUtil.set(testKey, testValue, 60);
        System.out.println("Set result: " + setResult);

        Object getValue = redisUtil.get(testKey);
        System.out.println("Get result: " + getValue);

        assert setResult : "Redis set operation should succeed";
        assert testValue.equals(getValue) : "Retrieved value should match stored value";

        System.out.println("Redis connection test passed!");
    }
}
