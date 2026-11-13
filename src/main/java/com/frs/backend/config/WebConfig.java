package com.frs.backend.config;
import org.springframework.context.annotation.*;import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer {@Override public void addCorsMappings(CorsRegistry registry){registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowedHeaders("*");}}
