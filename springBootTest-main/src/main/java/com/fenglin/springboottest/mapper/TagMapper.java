package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.dto.TagVO;
import com.fenglin.springboottest.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 全部标签 + 各自的已发布文章数 */
    @Select("select t.id, t.name, count(p.id) as postCount " +
            "from tags t " +
            "left join post_tags pt on pt.tag_id = t.id " +
            "left join posts p on p.id = pt.post_id and p.status = 1 " +
            "group by t.id, t.name " +
            "order by postCount desc, t.id")
    List<TagVO> selectWithPostCount();

    @Select("select id, name, created_at from tags where name = #{name} limit 1")
    Tag selectByName(@Param("name") String name);
}
