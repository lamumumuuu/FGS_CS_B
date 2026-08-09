// src/main/java/com/example/computerassociation/dto/LoginDTO.java

/**
 * 登录请求的数据传输对象
 * 接收前端登录表单提交的用户名和密码
 */

package com.example.computerassociation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")      // 验证注解：用户名不能为 null、空字符串或纯空格
    private String username;

    @NotBlank(message = "密码不能为空")        // 密码同理，不可为空
    private String password;
}