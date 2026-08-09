// src/main/java/com/example/computerassociation/annotation/RequiresPermission.java

/**
 * 自定义权限校验注解
 * 
 * 可用于 Controller 方法或类上，配合 AOP 切面实现方法级的权限控制。
 * value 为所需权限标识（如 "quest:create_global"），module 为所属模块（可选）。
 */

package com.example.computerassociation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})      // 可注解方法或类
@Retention(RetentionPolicy.RUNTIME)                  // 保留至运行时，以便反射读取
public @interface RequiresPermission {

    String value();                                  // 权限标识字符串，例如 "quest:create_global"

    String module() default "";                      // 所属模块，可选（如 "quest"、"member"）
}