package com.web.memoire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PythonConfig implements WebMvcConfigurer {

    /**
     * application.properties 에 지정된 로컬 이미지 저장 디렉터리 경로.
     * 예: src/main/resources/static/images 또는 D:/upload_files/memory_img
     */
    @Value("${app.static-images-dir}")
    private String imagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 클라이언트는 /upload_files/memory_img/** 로 호출
        String location = "file:///" + imagesDir.replace("\\", "/") + "/";
        registry.addResourceHandler("/upload_files/memory_img/**")
                .addResourceLocations(location);
    }
}