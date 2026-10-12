package com.descartes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(scanBasePackages = "com.descartes")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }


    @Configuration
    public class CorsConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST", "PUT", "DELETE");
        }
    }
}
