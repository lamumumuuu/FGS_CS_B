// src/main/java/com/example/computerassociation/mapper/UserMapper.java

/**
 * 用户 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法，无需手动编写 SQL。
 */

package com.example.computerassociation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.computerassociation.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 会自动生成常用的 CRUD 方法
}