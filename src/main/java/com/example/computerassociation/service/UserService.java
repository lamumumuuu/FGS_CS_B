package com.example.computerassociation.service;

import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.dto.UserDTO;
import com.example.computerassociation.entity.User;

/**
 * 用户服务接口
 * 定义用户相关的业务方法
 */
public interface UserService {

    /**
     * 用户注册
     * @param registerDTO 用户注册信息
     * @return 注册结果
     */
    boolean register(RegisterDTO registerDTO);

    /**
     * 用户登录
     * @param username 用户名或邮箱
     * @param password 密码
     * @return 用户对象，如果登录失败则返回null
     */
    User login(String username, String password);


    /**
     * 重置密码
     * @param email 邮箱
     * @param newPassword 新密码
     * @param verificationCode 验证码
     * @return 是否重置成功
     */
    boolean resetPassword(String email, String newPassword, String verificationCode);

    /**
     * 发送通用验证码
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String email);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    boolean sendResetPasswordEmail(String email);

    User getByUsername(String username);
}