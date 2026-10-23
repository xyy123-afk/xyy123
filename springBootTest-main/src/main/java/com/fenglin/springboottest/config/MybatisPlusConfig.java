package com.fenglin.springboottest.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>@MapperScan：告诉 Spring 去哪个包下面找 Mapper 接口并生成代理实现，
 * 少了它启动就会报 "required a bean of type 'XxxMapper' that could not be found"。
 *
 * <p>关于分页：本项目依赖的 MyBatis-Plus 3.5.12 发布物里没有 PaginationInnerInterceptor，
 * 所以分页没有用官方分页插件，而是在 PostMapper / CommentMapper 里手写 limit/offset 的 SQL 实现
 * （见 PostMapper.selectPublishedPage / countPublished），更直观也更好调试。
 */
@Configuration
@MapperScan("com.fenglin.springboottest.mapper")
public class MybatisPlusConfig {
}
