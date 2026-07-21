package com.example.computerassociation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.UserMapper;
import com.example.computerassociation.service.impl.UserServiceImpl;
import com.example.computerassociation.util.MailUtil;
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

import java.util.concurrent.TimeUnit;

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
        registerDTO.setEmail("test@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setCaptchaKey("captcha-key");
        registerDTO.setCaptchaCode("1234");
    }

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTest {

        @Test
        @DisplayName("注册成功 - 正常流程")
        void register_success() {
            when(redisUtil.getString("verification_code:test@example.com")).thenReturn("1234");
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userMapper.insert(any(User.class))).thenReturn(1);

            boolean result = userService.register(registerDTO);

            assertTrue(result);
            verify(redisUtil).del("verification_code:test@example.com");
        }

        @Test
        @DisplayName("注册失败 - 验证码已过期")
        void register_fail_captchaExpired() {
            when(redisUtil.getString("verification_code:test@example.com")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(registerDTO));
            assertEquals("验证码已过期", ex.getMessage());
        }

        @Test
        @DisplayName("注册失败 - 验证码错误")
        void register_fail_captchaWrong() {
            when(redisUtil.getString("verification_code:test@example.com")).thenReturn("5678");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(registerDTO));
            assertEquals("验证码错误", ex.getMessage());
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_fail_usernameExists() {
            when(redisUtil.getString("verification_code:test@example.com")).thenReturn("1234");
            when(userMapper.selectCount(argThat(w -> true))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(registerDTO));
            assertEquals("用户名已存在", ex.getMessage());
        }

        @Test
        @DisplayName("注册失败 - 邮箱已被注册")
        void register_fail_emailExists() {
            when(redisUtil.getString("verification_code:test@example.com")).thenReturn("1234");
            when(userMapper.selectCount(argThat(w -> true)))
                    .thenReturn(0L)
                    .thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(registerDTO));
            assertEquals("邮箱已被注册", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTest {

        @Test
        @DisplayName("登录成功 - 使用用户名")
        void login_success_withUsername() {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword("encodedPassword");

            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(userMapper.updateById(any(User.class))).thenReturn(1);

            User result = userService.login("testuser", "password123");

            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void login_fail_userNotFound() {
            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            User result = userService.login("nonexistent", "password123");

            assertNull(result);
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_fail_wrongPassword() {
            User user = new User();
            user.setPassword("encodedPassword");

            when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
            when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

            User result = userService.login("testuser", "wrongPassword");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("密码重置测试")
    class ResetPasswordTest {

        @Test
        @DisplayName("重置密码失败 - 邮箱不存在")
        void resetPassword_fail_emailNotExists() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.resetPassword("test@example.com", "newPass123", "123456"));
            assertEquals("邮箱不存在", ex.getMessage());
        }

        @Test
        @DisplayName("重置密码失败 - 验证码错误")
        void resetPassword_fail_wrongCode() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
            when(redisUtil.getString("reset_password_code:test@example.com")).thenReturn("654321");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.resetPassword("test@example.com", "newPass123", "123456"));
            assertEquals("验证码错误或已过期", ex.getMessage());
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

        @Test
        @DisplayName("邮箱存在")
        void existsByEmail_true() {
            when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

            assertTrue(userService.existsByEmail("existing@example.com"));
        }
    }
}
