// src/main/java/com/example/computerassociation/exception/BusinessException.java

/**
 * 自定义业务异常类
 * 用于在业务逻辑中抛出可识别的异常，携带业务状态码和提示信息。
 * 由 GlobalExceptionHandler 统一捕获并返回前端。
 */

package com.example.computerassociation.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;         /// 业务状态码（如 401, 403）

    public BusinessException(String message) {
        super(message);
        this.code = 500;                // 默认 500 服务器内部错误
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 快速构造默认 500 的异常 */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    /** 快速构造指定状态码的异常 */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }
}