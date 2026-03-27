package org.campus.classroom.mapper;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.campus.classroom.entity.User;

public interface UserMapper {
    @Select("select * from user where id = #{id}")
    User findById(Long id);

    @Select("select * from user where username = #{username} limit 1")
    User findByUsername(String username);

    @Insert("insert into user(username, password,nickname,email, role) values(#{username}, #{password}, #{nickname}, #{email},#{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
