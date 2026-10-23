package com.fenglin.springboottest.config;

import com.fenglin.springboottest.interceptor.AdminInterceptor;
import com.fenglin.springboottest.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：把守卫挂到需要登录的接口上。
 *
 * <p>/api/admin/** 只认站长（AdminInterceptor）；
 * /api/chat/** 任何登录用户都能用（LoginInterceptor）——私信是"我的"功能，
 * 游客不该访问，但也不该只有站长能用。
 *
 * <p>公开接口 /api/blog/** 不挂任何拦截器——博客前台本来就该匿名可读。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(AdminInterceptor adminInterceptor, LoginInterceptor loginInterceptor) {
        this.adminInterceptor = adminInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/chat/**");
    }
}
