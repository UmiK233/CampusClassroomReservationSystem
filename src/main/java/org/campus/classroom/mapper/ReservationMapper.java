package org.campus.classroom.mapper;

import org.apache.ibatis.annotations.*;
import org.campus.classroom.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface ReservationMapper {

    @Insert("""
            INSERT INTO reservation (
                user_id,resource_type,resource_id,classroom_id,reserve_date,start_time,end_time,reason,status)
            VALUES (
                #{userId},#{resourceType},#{resourceId},#{classroomId},#{reserveDate},#{startTime},#{endTime},#{reason},#{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Reservation reservation);

    @Select("SELECT * FROM reservation WHERE id=#{id}")
    @Results(id = "reservationResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "resource_type", property = "resourceType"),
            @Result(column = "resource_id", property = "resourceId"),
            @Result(column = "classroom_id", property = "classroomId"),
            @Result(column = "reserve_date", property = "reserveDate"),
            @Result(column = "start_time", property = "startTime"),
            @Result(column = "end_time", property = "endTime"),
            @Result(column = "reason", property = "reason"),
            @Result(column = "status", property = "status"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    Reservation selectById(Long id);

    @Select("SELECT * FROM reservation WHERE user_id=#{userId} ORDER BY create_time DESC")
    @ResultMap("reservationResultMap")
    List<Reservation> selectByUserId(Long userId);

    @Select("""
            SELECT *
            FROM reservation
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    @ResultMap("reservationResultMap")
    Reservation selectByReservationIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("""
            INSERT INTO reservation_usage (user_id, date, used_minutes)
            VALUES (#{userId}, #{date}, 0)
            ON DUPLICATE KEY UPDATE used_minutes = used_minutes
            """)
    //ON DUPLICATE KEY UPDATE used_minutes = used_minutes 处理重复插入的异常
    int initUsage(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Update("""
            UPDATE reservation_usage
            SET used_minutes = used_minutes + #{addMinutes}
            WHERE user_id = #{userId}
              AND date = #{date}
              AND used_minutes + #{addMinutes} <= 9*60
            """)
    int tryAddUsage(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("addMinutes") Long addMinutes);


    @Select("""
            SELECT *
            FROM reservation
            WHERE user_id = #{userId}
              AND status = 'ACTIVE'
              AND start_time >= #{startTime}
              AND start_time < #{endTime}
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectByUserIdAndTimeRange(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM reservation ORDER BY create_time DESC")
    @ResultMap("reservationResultMap")
    List<Reservation> selectAll();

    @Update("""
            UPDATE reservation
            SET status = 'CANCELLED'
            WHERE id = #{id}
              AND status = 'ACTIVE'
              AND end_time >= current_timestamp()
            """)
    int cancelReservation(@Param("id") Long id);

    @Update("""
            UPDATE reservation
            SET status = 'EXPIRED'
            WHERE status = 'ACTIVE'
              AND end_time < current_timestamp()
            """)
    int expireActiveReservations();


    @Select("""
            SELECT *
            FROM reservation
            WHERE user_id = #{userId}
              AND status = #{status}
            ORDER BY create_time DESC
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectByUserIdAndStatus(@Param("userId") Long userId,
                                              @Param("status") String status);

    @Select("""
            SELECT *
            FROM reservation
            WHERE user_id = #{userId}
              AND status != #{status}
            ORDER BY create_time DESC
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectByUserIdAndNotStatus(@Param("userId") Long userId,
                                                 @Param("status") String status);

    @Select("""
            SELECT *
            FROM reservation
            WHERE resource_type='SEAT'
              AND resource_id=#{seatId}
              AND status = 'ACTIVE'
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            FOR UPDATE
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectSeatConflictsForUpdate(@Param("seatId") Long seatId,
                                                   @Param("startTime") LocalDateTime startTime,//开始时间必须在其他预约中的结束之前之后,否则冲突, end_time > #{startTime} 就冲突
                                                   @Param("endTime") LocalDateTime endTime);//结束时间必须在其他预约中的开始时间之前,否则冲突, start_time < #{endTime} 就冲突

    @Select("""
            SELECT *
            FROM reservation
            WHERE resource_type='CLASSROOM'
              AND resource_id=#{classroomId}
              AND status = 'ACTIVE'
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            FOR UPDATE
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectClassroomConflictsForUpdate(@Param("classroomId") Long classroomId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    //查询classroom预约时有无seat被预约
    @Select("""
            SELECT *
            FROM reservation
            WHERE classroom_id = #{classroomId}
              AND resource_type = 'SEAT'
              AND status = 'ACTIVE'
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            FOR UPDATE
            """)
    @ResultMap("reservationResultMap")
    List<Reservation> selectSeatConflictsInClassroomForUpdate(@Param("classroomId") Long classroomId,
                                                              @Param("startTime") LocalDateTime startTime,
                                                              @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT count(*)
            FROM reservation
            WHERE user_id=#{userId}
              AND resource_type = 'SEAT'
              AND status = 'ACTIVE'
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            FOR UPDATE
            """)
    int selectStudentTimeConflictForUpdate(@Param("userId") Long userId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT count(*)
            FROM reservation
            WHERE resource_type='SEAT'
              AND resource_id=#{seatId}
              AND status = 'ACTIVE'
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            """)
    long countActiveSeatConflict(Long seatId, LocalDateTime startTime, LocalDateTime endTime);
}
