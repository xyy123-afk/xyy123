package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 Mapper。
 *
 * <p>继承 BaseMapper 后免费获得 insert / selectById / updateById / deleteById /
 * selectList(LambdaQueryWrapper) 等基础方法，不用写任何 XML。
 * 下面两个是自定义查询：用 @Select 注解写 SQL，比 XML 更直观，适合这种简单语句。
 *
 * <p>扫描方式见 config/MybatisPlusConfig 的 @MapperScan。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 按用户名查用户（用户名有唯一索引，最多一条，加 limit 1 更保险） */
    @Select("select id, username, password, nickname, avatar, created_at from users where username = #{username} limit 1")
    User selectByUsername(@Param("username") String username);

    /** 统计同名用户数量，注册时用来判重（>0 就是已存在） */
    @Select("select count(1) from users where username = #{username}")
    long countByUsername(@Param("username") String username);

    /**
     * 私信用：按登录名或昵称模糊找人，排除自己，最多 20 条。
     * 关键字为空时返回"其它用户"，方便新账号直接挑一个人开聊。
     */
    @Select("<script>" +
            "select id, username, nickname, avatar from users where id != #{meId} " +
            "<if test='kw != null and kw != \"\"'>" +
            "and (username like concat('%', #{kw}, '%') or nickname like concat('%', #{kw}, '%')) " +
            "</if>" +
            "order by id asc limit 20" +
            "</script>")
    java.util.List<User> searchChatUsers(@Param("meId") Long meId, @Param("kw") String kw);
}
