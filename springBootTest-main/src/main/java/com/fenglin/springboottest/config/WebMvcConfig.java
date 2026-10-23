package com.fenglin.springboottest.config;

import com.fenglin.springboottest.interceptor.AdminInterceptor;
import com.fenglin.springboottest.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 层配置：把守卫挂到需要登录的接口上。
 *
 * <p>/api/admin/** 只认站长（AdminInterceptor）；
 * /api/chat/** 任何登录用户都能用（LoginInterceptor）——私信是"我的"功能，
 * 游客不该访问，但也不该只有站长能用；
 * /api/upload 是头像/表情包上传，同样需要登录，防止匿名乱传文件。
 *
 * <p>公开接口 /api/blog/** 不挂任何拦截器——博客前台本来就该匿名可读。
 * /uploads/** 是上传文件的静态映射（头像/表情包图片），必须匿名可读，否则浏览器加载不了图。
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
                .addPathPatterns("/api/chat/**", "/api/upload");
    }

    /**
     * 上传文件静态映射：/uploads/** -> 项目根 uploads/ 目录。
     * file: 前缀让 Spring 去文件系统找，而不是 classpath。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get("uploads").toAbsolutePath().normalize().toString()
                        .replace("\\", "/") + "/");
    }
}
