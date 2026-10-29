package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SiteSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站点设置 Mapper。单行表，id 恒为 1，BaseMapper 的 selectById/updateById 就够用，
 * 不需要写自定义 SQL。
 */
@Mapper
public interface SiteSettingMapper extends BaseMapper<SiteSetting> {
}
