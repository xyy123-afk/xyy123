package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.dto.CategoryVO;
import com.fenglin.springboottest.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /** 全部分类 + 各自的已发布文章数（一条 join 查完，避免 N+1） */
    @Select("select c.id, c.name, c.slug, c.sort_order, count(p.id) as postCount " +
            "from categories c " +
            "left join posts p on p.category_id = c.id and p.status = 1 " +
            "group by c.id, c.name, c.slug, c.sort_order " +
            "order by c.sort_order, c.id")
    List<CategoryVO> selectWithPostCount();
}
