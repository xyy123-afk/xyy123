package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.GalgameSave;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Galgame 存档 Mapper。
 *
 * <p>一人一档，靠 galgame_saves 的 user_id 唯一索引兜底；
 * 保存用「先查、有则更新，无则插入」，并发下最多被索引挡重复，不会产生两份存档。
 */
@Mapper
public interface GalgameSaveMapper extends BaseMapper<GalgameSave> {

    /** 查我的存档，没有返回 null */
    @Select("select * from galgame_saves where user_id = #{userId}")
    GalgameSave selectByUserId(@Param("userId") Long userId);

    /** 删除我的存档（重新开始 / 清档用） */
    @Delete("delete from galgame_saves where user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
