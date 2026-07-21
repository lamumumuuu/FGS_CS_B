package com.example.computerassociation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
