// src/main/java/com/example/computerassociation/mapper/RolePermissionMapper.java

/**
 * 角色权限关联 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，暂无自定义查询。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}