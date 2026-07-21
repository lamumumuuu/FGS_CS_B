package com.example.computerassociation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
public class ComputerAssociationApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(ComputerAssociationApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
