// src/main/java/com/example/computerassociation/service/UserService.java

/**
 * 用户服务接口
 * 继承 MyBatis-Plus IService，自动获得基础 CRUD。
 * 扩展注册、登录等认证相关方法。
 */

package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.entity.User;

public interface UserService extends IService<User> {

    /** 用户注册（默认分配外门弟子角色） */
    boolean register(RegisterDTO registerDTO);

    /** 注册并直接指定角色和峰 */
    boolean registerWithRole(User user, String roleName, Long peakId);

    /** 用户登录验证，成功返回用户对象，失败返回 null */
    User login(String username, String password);

    /** 判断用户名是否已存在 */
    boolean existsByUsername(String username);

    /** 根据用户名获取用户 */
    User getByUsername(String username);
}