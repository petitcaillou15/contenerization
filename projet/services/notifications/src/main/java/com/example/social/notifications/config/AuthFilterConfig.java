package com.example.social.notifications.config;

import com.example.social.notifications.auth.AuthFilter;
import com.example.social.notifications.users.UsersClient;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(UsersClient usersClient) {
        var reg = new FilterRegistrationBean<>(new AuthFilter(usersClient));
        reg.addUrlPatterns("/notifications", "/notifications/*");
        reg.setName("authFilter");
        return reg;
    }
}
