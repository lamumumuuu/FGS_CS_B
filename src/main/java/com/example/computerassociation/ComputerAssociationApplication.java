// src/main/java/com/example/computerassociation/ComputerAssociationApplication.java

/**
 * Spring Boot 启动类 —— 计算机协会管理系统
 * 
 * 1. 标记为 Spring Boot 应用的入口。
 * 2. 启动异步任务支持。
 * 3. 启动成功后打印 ASCII Art 横幅与 Swagger 文档地址。
 * 4. 定义密码编码器 Bean（供 Spring Security 使用）。
 */

package com.example.computerassociation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j                                      /// 启用 Slf4j 日志，类中可直接使用 log 对象
@EnableAsync                                /// 启用 Spring 异步任务（如 @Async 方法）
@SpringBootApplication                      /// 组合注解：@Configuration + @EnableAutoConfiguration + @ComponentScan
public class ComputerAssociationApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(ComputerAssociationApplication.class, args);  /// 启动 Spring Boot
    }

    /**
     * 应用启动完成后执行
     * 打印 ASCⅡ Art 横幅，表示服务启动成功，并提示 Swagger 文档地址
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("\n" +
                "\n" +
                "  ██████╗ ██████╗ ███████╗ █████╗ ████████╗██╗██╗   ██╗████████╗███████╗\n" +
                "  ██╔══██╗██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║██║   ██║╚══██╔══╝██╔════╝\n" +
                "  ██║  ██║██████╔╝█████╗  ███████║   ██║   ██║██║   ██║   ██║   █████╗  \n" +
                "  ██║  ██║██╔══██╗██╔══╝  ██╔══██║   ██║   ██║╚██╗ ██╔╝   ██║   ██╔══╝  \n" +
                "  ██████╔╝██║  ██║███████╗██║  ██║   ██║   ██║ ╚████╔╝    ██║   ███████╗\n" +
                "  ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═══╝     ╚═╝   ╚══════╝\n" +
                "\n" +
                "  >>> 计算机协会管理系统启动成功 <<<\n" +
                "  >>> Swagger API 文档: http://localhost:8080/swagger-ui.html <<<\n" +
                "\n");
    }
}