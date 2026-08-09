// src/main/java/com/example/computerassociation/common/Result.java

/**
 * 统一 API 返回结果包装类
 * 
 * 所有 Controller 返回值均使用此类包装，前端接收到的 JSON 格式统一为：
 * { "code": 200, "message": "操作成功", "data": {...} }
 * 
 * @param <T> 响应数据的类型
 */

package com.example.computerassociation.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;       /// 业务状态码（200 成功，其他为错误）
    private String message;     /// 提示消息
    private T data;             /// 响应数据，可为 null

    private Result() {}         /// 构造器私有，强制通过静态工厂方法创建

    // ========== 成功响应 ==========

    /** 返回默认成功消息的操作结果 */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /** 返回自定义成功消息的操作结果 */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // ========== 失败响应 ==========

    /** 返回指定状态码和错误消息的失败结果 */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    /** 返回默认 500 错误码的失败结果 */
    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }

    /** 返回默认 500 错误码及默认错误消息的失败结果 */
    public static <T> Result<T> fail() {
        return fail(500, "操作失败");
    }
}