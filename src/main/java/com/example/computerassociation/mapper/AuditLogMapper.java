// src/main/java/com/example/computerassociation/mapper/AuditLogMapper.java

/**
 * 审计日志 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法。
 * 暂无自定义查询。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}