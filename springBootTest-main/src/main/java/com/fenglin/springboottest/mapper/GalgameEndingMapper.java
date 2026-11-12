package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.GalgameEnding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 结局收集 Mapper。
 *
 * <p>插入时若撞上 (user_id, ending_id) 唯一索引，说明这个人早就打通过这个结局，
 * 调用方捕获 DuplicateKeyException 当作「已收集」处理即可，不用先查后插。
 */
@Mapper
public interface GalgameEndingMapper extends BaseMapper<GalgameEnding> {

    /** 我打通过哪些结局（去重后的 ending_id 列表） */
    @Select("select ending_id from galgame_endings where user_id = #{userId} order by created_at")
    List<String> selectEndingIdsByUser(@Param("userId") Long userId);

    /** 全站每个结局有多少人达成：一行是 [ending_id, count] */
    @Select("select ending_id, count(1) as cnt from galgame_endings group by ending_id")
    List<java.util.Map<String, Object>> countGroupByEnding();
}
