// src/main/java/com/example/computerassociation/dto/RegisterDTO.java

/**
 * 注册请求的数据传输对象
 * 接收前端注册表单提交的用户名和密码，并附带长度校验
 */

package com.example.computerassociation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度为2-20个字符")   // 长度限制，防止过短或过长的用户名
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")    // 密码长度限制，确保一定安全性
    private String password;

    private String peak;
}