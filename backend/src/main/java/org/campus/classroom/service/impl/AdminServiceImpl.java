package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Reservation;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.entity.User;
import org.campus.classroom.enums.ReservationStatus;
import org.campus.classroom.enums.ResourceType;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.AttendanceMapper;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.ReservationMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.service.AdminService;
import org.campus.classroom.service.NotificationService;
import org.campus.classroom.vo.AdminReservationVO;
import org.campus.classroom.vo.AdminUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final ClassroomMapper classroomMapper;
    private final SeatMapper seatMapper;
    private final AttendanceMapper attendanceMapper;
    private final NotificationService notificationService;

    @Override
    public List<AdminUserVO> listUsers(String keyword, String role, Integer status) {
        return userMapper.selectAdminList(normalize(keyword), normalize(role), status)
                .stream()
                .map(this::toUserVO)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserVO updateUserStatus(Long adminUserId, Long targetUserId, Integer status, String reason) {
        if (Objects.equals(adminUserId, targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "管理员不能修改自己的状态");
        }
        User user = getUser(targetUserId);
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能通过此接口修改管理员状态");
        }
        if (Objects.equals(user.getStatus(), status)) {
            return toUserVO(user);
        }
        int updatedRows = userMapper.updateStatus(targetUserId, status);
        if (updatedRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "用户状态更新失败，请刷新后重试");
        }

        String title = status == 0 ? "账号已被封禁" : "账号已恢复使用";
        String content = buildUserStatusNotice(status, reason);
        notificationService.createSystemNotification(targetUserId, "USER_STATUS", title, content);

        log.info("[管理员更新用户状态成功] 管理员ID={}, 目标用户ID={}, 新状态={}", adminUserId, targetUserId, status);
        return toUserVO(getUser(targetUserId));
    }

    @Override
    public List<AdminReservationVO> listReservations(String keyword, String status) {
        List<Reservation> reservations = reservationMapper.selectAdminList(normalize(keyword), normalize(status));
        Set<Long> userIds = reservations.stream().map(Reservation::getUserId).collect(Collectors.toSet());
        Set<Long> classroomIds = reservations.stream().map(Reservation::getClassroomId).collect(Collectors.toSet());
        Set<Long> seatIds = reservations.stream()
                .filter(item -> ResourceType.SEAT.name().equals(item.getResourceType()))
                .map(Reservation::getResourceId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userIds.stream()
                .map(userMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Classroom> classroomMap = classroomIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectByIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity()));
        Map<Long, Seat> seatMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatMapper.selectByIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));

        return reservations.stream()
                .map(item -> toReservationVO(item, userMap, classroomMap, seatMap))
                .filter(item -> matchReservationKeyword(item, keyword))
                .toList();
    }

    @Override
    @Transactional
    public void cancelReservation(Long adminUserId, Long reservationId, String reason) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约不存在");
        }
        if (!ReservationStatus.ACTIVE.name().equals(reservation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅可取消进行中的预约");
        }

        int cancelRows = reservationMapper.adminCancelReservation(reservationId);
        if (cancelRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "预约状态已变化，请刷新后重试");
        }

        Long minusMinutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        int usageRows = reservationMapper.minusUsage(reservation.getUserId(), reservation.getReserveDate(), minusMinutes);
        if (usageRows != 1) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "回滚预约额度失败，请联系管理员");
        }

        attendanceMapper.insertStatusIfAbsent(reservationId, "CANCELLED");
        attendanceMapper.updateStatusIfPending(reservationId, "CANCELLED");

        notificationService.createSystemNotification(
                reservation.getUserId(),
                "RESERVATION_CANCELLED",
                "预约已被管理员取消",
                buildReservationCancelNotice(reason)
        );

        log.info("[管理员取消预约成功] 管理员ID={}, 预约ID={}", adminUserId, reservationId);
    }

    private User getUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private AdminUserVO toUserVO(User user) {
        AdminUserVO userVO = new AdminUserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    private AdminReservationVO toReservationVO(Reservation reservation,
                                               Map<Long, User> userMap,
                                               Map<Long, Classroom> classroomMap,
                                               Map<Long, Seat> seatMap) {
        AdminReservationVO reservationVO = new AdminReservationVO();
        BeanUtils.copyProperties(reservation, reservationVO);

        User user = userMap.get(reservation.getUserId());
        if (user != null) {
            reservationVO.setUsername(user.getUsername());
            reservationVO.setNickname(user.getNickname());
        }

        Classroom classroom = classroomMap.get(reservation.getClassroomId());
        if (classroom != null) {
            String resourceName = classroom.getBuilding() + " " + classroom.getRoomNumber();
            if (ResourceType.SEAT.name().equals(reservation.getResourceType())) {
                Seat seat = seatMap.get(reservation.getResourceId());
                if (seat != null) {
                    resourceName = resourceName + " " + seat.getSeatNumber();
                }
            }
            reservationVO.setResourceName(resourceName);
        }
        return reservationVO;
    }

    private boolean matchReservationKeyword(AdminReservationVO reservation, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(reservation.getUsername(), normalized)
                || contains(reservation.getNickname(), normalized)
                || contains(reservation.getResourceName(), normalized)
                || contains(reservation.getReason(), normalized)
                || contains(String.valueOf(reservation.getId()), normalized);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildUserStatusNotice(Integer status, String reason) {
        String action = status == 0
                ? "您的账号已被管理员封禁，当前无法继续登录和发起新的预约。"
                : "您的账号已被管理员恢复，可正常登录和使用预约功能。";
        if (!StringUtils.hasText(reason)) {
            return action;
        }
        return action + "\n原因：" + reason.trim();
    }

    private String buildReservationCancelNotice(String reason) {
        String content = "您的预约已被管理员取消，请重新选择时间或联系管理员。";
        if (!StringUtils.hasText(reason)) {
            return content;
        }
        return content + "\n原因：" + reason.trim();
    }
}
