package com.example.computerassociation.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.dto.LoginDTO;
import com.example.computerassociation.dto.RegisterDTO;
import com.example.computerassociation.dto.ResetPasswordDTO;
import com.example.computerassociation.dto.SendCodeDTO;
import com.example.computerassociation.entity.User;
import com.example.computerassociation.service.UserService;
import com.example.computerassociation.util.JwtUtil;
import com.example.computerassociation.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "用户管理", description = "用户注册、登录、密码重置等接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Operation(summary = "获取图片验证码", description = "生成图形验证码，返回验证码key和Base64图片")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "获取成功"))
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 150);
        String captchaCode = lineCaptcha.getCode();
        String captchaImage = lineCaptcha.getImageBase64();
        String captchaKey = UUID.randomUUID().toString();

        redisUtil.set(captchaKey, captchaCode, 5, TimeUnit.MINUTES);

        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", captchaKey);
        data.put("captchaImage", "data:image/png;base64," + captchaImage);

        return Result.success(data);
    }

    @Operation(summary = "用户注册", description = "使用用户名、邮箱和验证码注册新用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或验证码错误")
    })
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        boolean success = userService.register(registerDTO);
        return success ? Result.success("注册成功") : Result.fail("注册失败");
    }

    @Operation(summary = "用户登录", description = "使用用户名/邮箱和密码登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功，返回JWT令牌"),
            @ApiResponse(responseCode = "400", description = "用户名或密码错误")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        if (user != null) {
            String token = jwtUtil.generateToken(user.getUsername());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);
            return Result.success(data, "登录成功");
        }
        return Result.fail("用户名或密码错误");
    }

    @Operation(summary = "发送注册验证码", description = "向邮箱发送注册验证码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "400", description = "邮箱已被注册")
    })
    @PostMapping("/send-code")
    public Result<String> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        boolean success = userService.sendVerificationCode(sendCodeDTO.getEmail());
        return success ? Result.success("验证码已发送至您的邮箱") : Result.fail("验证码发送失败，请稍后再试");
    }

    @Operation(summary = "发送重置密码验证码", description = "向邮箱发送重置密码验证码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "400", description = "邮箱不存在")
    })
    @PostMapping("/send-reset-code")
    public Result<String> sendResetCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        boolean success = userService.sendResetPasswordEmail(sendCodeDTO.getEmail());
        return success ? Result.success("验证码已发送至您的邮箱") : Result.fail("验证码发送失败，请稍后再试");
    }

    @Operation(summary = "重置密码", description = "使用邮箱、新密码和验证码重置密码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "400", description = "验证码错误或邮箱不存在")
    })
    @PutMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        boolean success = userService.resetPassword(
                resetPasswordDTO.getEmail(),
                resetPasswordDTO.getNewPassword(),
                resetPasswordDTO.getVerificationCode()
        );
        return success ? Result.success("密码重置成功") : Result.fail("密码重置失败");
    }

    @Operation(summary = "获取用户信息", description = "通过JWT令牌获取当前用户信息", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "令牌无效或已过期")
    })
    // @GetMapping("/info")
        @GetMapping("/me")
    public Result<User> getUserInfo(
            @Parameter(description = "JWT令牌", required = true)
            @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String username = jwtUtil.getUsernameFromToken(token);
        if (username != null && jwtUtil.validateToken(token, username)) {
            User user = userService.getByUsername(username);
            if (user != null) {
                user.setPassword(null);
                return Result.success(user);
            }
            return Result.fail("用户不存在");
        }
        return Result.fail(401, "令牌无效或已过期");
    }
}
