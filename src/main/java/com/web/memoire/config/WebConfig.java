package com.web.memoire.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Token-Expired", "Authorization", "RefreshToken")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) { // ✅ 맞음

        registry.addResourceHandler("/upload_files/user_profile/**")
                .addResourceLocations("file:///D:/upload_files/user_profile/");

        registry.addResourceHandler("/upload_files/memory_img/**")
                .addResourceLocations("file:///D:/upload_files/memory_img/");

        registry.addResourceHandler("/upload_files/memory_video/**")
                .addResourceLocations("file:///D:/upload_files/memory_video/");
    }
}
