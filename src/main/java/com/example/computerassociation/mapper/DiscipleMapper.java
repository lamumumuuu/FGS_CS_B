package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.Disciple;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiscipleMapper extends BaseMapper<Disciple> {
    // 继承 BaseMapper 即可获得 CRUD 方法，无需额外 SQL
}