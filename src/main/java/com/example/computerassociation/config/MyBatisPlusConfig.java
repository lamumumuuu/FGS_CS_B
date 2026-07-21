package com.example.computerassociation.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * 配置相关设置
 */
@Configuration
@MapperScan("com.example.computerassociation.mapper") // 扫描Mapper接口
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     * @return MyBatis-Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }
}
