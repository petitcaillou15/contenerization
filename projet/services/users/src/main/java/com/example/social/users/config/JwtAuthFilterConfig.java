package com.example.social.users.config;

import com.example.social.users.auth.JwtAuthFilter;
import com.example.social.users.auth.JwtService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtService jwtService) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(new JwtAuthFilter(jwtService));
        reg.addUrlPatterns("/verify", "/users/me");
        reg.setName("jwtAuthFilter");
        return reg;
    }
}
