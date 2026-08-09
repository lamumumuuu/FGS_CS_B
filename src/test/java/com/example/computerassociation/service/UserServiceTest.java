package com.example.computerassociation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.UserMapper;
import com.example.computerassociation.service.impl.UserServiceImpl;
import com.example.computerassociation.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
    }

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTest {

        @Test
        @DisplayName("注册成功 - 正常流程")
        void register_success() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userMapper.insert(any(User.class))).thenReturn(1);

            boolean result = userService.register(registerDTO);

            assertTrue(result);
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_fail_usernameExists() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(registerDTO));
            assertEquals("用户名已存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTest {

        @Test
        @DisplayName("登录成功 - 使用用户名")
        void login_success_withUsername() {
            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword("encodedPassword");
            user.setStatus(1);

            when(redisUtil.isLoginLocked("testuser")).thenReturn(false);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            User result = userService.login("testuser", "password123");

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            verify(redisUtil).clearLoginFailure("testuser");
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void login_fail_userNotFound() {
            when(redisUtil.isLoginLocked("nonexistent")).thenReturn(false);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            User result = userService.login("nonexistent", "password123");

            assertNull(result);
            verify(redisUtil).recordLoginFailure("nonexistent");
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_fail_wrongPassword() {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword("encodedPassword");
            user.setStatus(1);

            when(redisUtil.isLoginLocked("testuser")).thenReturn(false);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            User result = userService.login("testuser", "wrongPassword");

            assertNull(result);
            verify(redisUtil).recordLoginFailure("testuser");
        }

        @Test
        @DisplayName("登录失败 - 账户被锁定")
        void login_fail_accountLocked() {
            when(redisUtil.isLoginLocked("testuser")).thenReturn(true);
            when(redisUtil.getLoginLockRemainingTime("testuser")).thenReturn(900L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login("testuser", "password123"));
            assertTrue(ex.getMessage().contains("账户已被锁定"));
        }

        @Test
        @DisplayName("登录失败 - 账户已被禁用")
        void login_fail_accountDisabled() {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword("encodedPassword");
            user.setStatus(0);

            when(redisUtil.isLoginLocked("testuser")).thenReturn(false);
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.login("testuser", "password123"));
            assertEquals("账户已被禁用", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("检查用户存在性测试")
    class ExistsTest {

        @Test
        @DisplayName("用户名存在")
        void existsByUsername_true() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

            assertTrue(userService.existsByUsername("existinguser"));
        }

        @Test
        @DisplayName("用户名不存在")
        void existsByUsername_false() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

            assertFalse(userService.existsByUsername("nonexistentuser"));
        }
    }
}
