package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Link;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 友链 Mapper：通用 CRUD 走 BaseMapper，列表按 sort_order 排序。 */
@Mapper
public interface LinkMapper extends BaseMapper<Link> {

    /** 前台友链页 / 后台列表都按这个顺序出：排序值小的在前，同序按创建时间倒序 */
    @Select("select * from links order by sort_order asc, id desc")
    List<Link> selectAllOrdered();
}
