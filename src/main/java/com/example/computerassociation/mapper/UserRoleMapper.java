// src/main/java/com/example/computerassociation/mapper/UserRoleMapper.java

/**
 * 用户角色关联 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，暂无自定义查询。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}