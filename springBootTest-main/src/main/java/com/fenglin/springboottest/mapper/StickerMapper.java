package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Sticker;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表情包收藏 Mapper。基础 CRUD 直接用 MyBatis-Plus 的 BaseMapper 免费方法，
 * 这里不需要自定义 SQL。
 */
@Mapper
public interface StickerMapper extends BaseMapper<Sticker> {
}
