package org.campus.classroom.mapper;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.campus.classroom.entity.User;

public interface UserMapper {
    @Select("select * from users where uid = #{id}")
    User getUserById(String id);
}
