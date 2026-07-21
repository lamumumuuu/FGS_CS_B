package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，获得基本的CRUD操作
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus会自动生成常用的CRUD方法，无需手动编写SQL
}
