package com.example.social.posts.config;

import com.example.social.posts.auth.AuthFilter;
import com.example.social.posts.users.UsersClient;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilter(UsersClient usersClient) {
        var reg = new FilterRegistrationBean<>(new AuthFilter(usersClient));
        reg.addUrlPatterns("/posts", "/posts/*");
        reg.setName("authFilter");
        return reg;
    }
}
