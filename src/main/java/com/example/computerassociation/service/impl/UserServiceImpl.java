package com.example.computerassociation.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.UserMapper;
import com.example.computerassociation.service.UserService;
import com.example.computerassociation.util.MailUtil;
import com.example.computerassociation.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    private static final int VERIFICATION_CODE_EXPIRE_TIME = 5;

    @Override
    public boolean register(RegisterDTO registerDTO) {
        String code = redisUtil.getString("verification_code:" + registerDTO.getEmail());
        if (code == null) {
            throw BusinessException.of("验证码已过期");
        }
        if (!code.equalsIgnoreCase(registerDTO.getCaptchaCode())) {
            throw BusinessException.of("验证码错误");
        }
        redisUtil.del("verification_code:" + registerDTO.getEmail());

        if (existsByUsername(registerDTO.getUsername())) {
            throw BusinessException.of("用户名已存在");
        }

        if (existsByEmail(registerDTO.getEmail())) {
            throw BusinessException.of("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        return save(user);
    }

    @Override
    public User login(String username, String password) {
        if (redisUtil.isLoginLocked(username)) {
            long remainingTime = redisUtil.getLoginLockRemainingTime(username);
            long remainingMinutes = remainingTime / 60;
            throw BusinessException.of("账户已被锁定，请 " + remainingMinutes + " 分钟后重试");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).or().eq("email", username);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            redisUtil.recordLoginFailure(username);
            return null;
        }

        redisUtil.clearLoginFailure(username);

        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    @Override
    public boolean resetPassword(String email, String newPassword, String verificationCode) {
        if (!existsByEmail(email)) {
            throw BusinessException.of("邮箱不存在");
        }

        String storedCode = redisUtil.getString("reset_password_code:" + email);

        if (storedCode == null || !storedCode.equals(verificationCode)) {
            throw BusinessException.of("验证码错误或已过期");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);

        User user = new User();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());

        int result = userMapper.update(user, queryWrapper);

        redisUtil.del("reset_password_code:" + email);

        return result > 0;
    }

    @Override
    public boolean sendResetPasswordEmail(String email) {
        if (!existsByEmail(email)) {
            throw BusinessException.of("邮箱不存在");
        }

        if (!redisUtil.canSendCode(email)) {
            long remainingTime = redisUtil.getCodeSendRemainingTime(email);
            throw BusinessException.of("请求过于频繁，请 " + remainingTime + " 秒后重试");
        }

        String verificationCode = RandomUtil.randomNumbers(6);

        boolean success = redisUtil.setVerificationCode("reset_password_code:" + email, verificationCode,
                VERIFICATION_CODE_EXPIRE_TIME, TimeUnit.MINUTES);

        if (!success) {
            throw BusinessException.of("验证码存储失败，请稍后重试");
        }

        redisUtil.recordCodeSent(email);

        try {
            MailUtil.sendVerificationEmail(email, "重置密码验证码", verificationCode);
            return true;
        } catch (Exception e) {
            log.error("发送重置密码邮件失败: email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return userMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean sendVerificationCode(String email) {
        if (existsByEmail(email)) {
            throw BusinessException.of("邮箱已被注册");
        }

        if (!redisUtil.canSendCode(email)) {
            long remainingTime = redisUtil.getCodeSendRemainingTime(email);
            throw BusinessException.of("请求过于频繁，请 " + remainingTime + " 秒后重试");
        }

        String verificationCode = RandomUtil.randomNumbers(6);

        boolean success = redisUtil.setVerificationCode("verification_code:" + email, verificationCode,
                VERIFICATION_CODE_EXPIRE_TIME, TimeUnit.MINUTES);

        if (!success) {
            throw BusinessException.of("验证码存储失败，请稍后重试");
        }

        redisUtil.recordCodeSent(email);

        try {
            MailUtil.sendVerificationEmail(email, "您的验证码", verificationCode);
            return true;
        } catch (Exception e) {
            log.error("发送验证码邮件失败: email={}", email, e);
            return false;
        }
    }

    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }
}
