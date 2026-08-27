package com.ofood.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String storageRootPath;

    public WebConfig(@Value("${app.storage.local.root:/tmp/ofood/uploads}") String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootLocation = Paths.get(storageRootPath).toAbsolutePath().normalize();
        String absoluteResourcePath = rootLocation.toUri().toString();
        if (!absoluteResourcePath.endsWith("/")) {
            absoluteResourcePath += "/";
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteResourcePath);
    }
}
