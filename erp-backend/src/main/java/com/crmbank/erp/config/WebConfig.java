package com.crmbank.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 스토리지 파일은 StorageController(@RestController)에서 직접 컨트롤 및 로깅 처리
        registry.addResourceHandler("/null-resource-path/**")
                .addResourceLocations("file:/dev/null/");
    }
}
