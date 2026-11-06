package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Image;
import org.apache.ibatis.annotations.Mapper;

/**
 * images 表的数据访问层。
 *
 * <p>只需继承 BaseMapper：上传是 insert（自增 id 会回填到 image.id），
 * 读图是 selectById，没有复杂查询，不用写 XML。
 * 扫描方式见 config/MybatisPlusConfig 的 @MapperScan。
 */
@Mapper
public interface ImageMapper extends BaseMapper<Image> {
}
