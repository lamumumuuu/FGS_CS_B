// src/main/java/com/example/computerassociation/annotation/RequiresRole.java

/**
 * 自定义角色校验注解
 * 
 * 用于方法或类上，配合 AOP 切面实现角色级别的访问控制。
 * 支持指定多个角色，并提供逻辑判断（满足任一角色 或 必须满足所有角色）。
 */

package com.example.computerassociation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})      // 可用于方法或类
@Retention(RetentionPolicy.RUNTIME)                  // 保留至运行时，供 AOP 反射读取
public @interface RequiresRole {

    String[] value();                                // 所需角色标识数组，例如 {"sect_master", "grand_elder"}

    Logical logical() default Logical.ANY;           // 角色判断逻辑：ANY（任一满足）或 ALL（全部满足）

    /** 逻辑判断枚举 */
    enum Logical {
        ANY,                                         // 拥有任意一个角色即通过
        ALL                                          // 必须拥有全部角色才通过
    }
}