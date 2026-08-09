package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.LingshiTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 灵石流水数据访问层
 */
@Mapper
public interface LingshiTransactionMapper extends BaseMapper<LingshiTransaction> {

    /**
     * 获取指定弟子的灵石流水
     */
    @Select("SELECT * FROM lingshi_transactions WHERE disciple_id = #{discipleId} ORDER BY created_at DESC LIMIT 100")
    List<LingshiTransaction> selectByDiscipleId(@Param("discipleId") Long discipleId);

    /**
     * 根据类型获取流水
     */
    @Select("SELECT * FROM lingshi_transactions WHERE type = #{type} ORDER BY created_at DESC LIMIT 100")
    List<LingshiTransaction> selectByType(@Param("type") String type);
}
