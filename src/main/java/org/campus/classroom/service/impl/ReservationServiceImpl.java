package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Reservation;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ClassroomStatus;
import org.campus.classroom.enums.ReservationStatus;
import org.campus.classroom.enums.ResourceType;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.enums.SeatStatus;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.ReservationMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.vo.ReservationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    @Transactional
    public Long createSeatReservation(Long currentUserId, SeatReservationCreateDTO request) {
        log.info("[座位预约开始] userId={}, request={}", currentUserId, request);

        validateTime(request.getStartTime(), request.getEndTime());

        int count = reservationMapper.selectStudentTimeConflictForUpdate(
                currentUserId,
                request.getStartTime(),
                request.getEndTime()
        );
        if (count >= 1) {
            log.warn("[预约冲突] 用户已有预约 userId={}, start={}, end={}",
                    currentUserId, request.getStartTime(), request.getEndTime());
            throw new BusinessException(ResultCode.CONFLICT, "同一时间段只能有一个预约");
        }

        Long classroomId = checkSeatReservableAndGetClassroomId(request);
        Reservation reservation = buildSeatReservation(currentUserId, request, classroomId);
        reservationMapper.insert(reservation);

        log.info("[座位预约成功] userId={}, reservationId={}, seatId={}",
                currentUserId, reservation.getId(), request.getSeatId());
        return reservation.getId();
    }

    @Override
    @Transactional
    public Long createClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request) {
        log.info("[教室预约开始] userId={}, request={}", currentUserId, request);

        validateTime(request.getStartTime(), request.getEndTime());
        checkClassroomReservable(request);

        Reservation reservation = buildClassroomReservation(currentUserId, request);
        reservationMapper.insert(reservation);

        log.info("[教室预约成功] userId={}, reservationId={}, classroomId={}",
                currentUserId, reservation.getId(), request.getClassroomId());
        return reservation.getId();
    }

    @Override
    @Transactional
    public Boolean cancelReservation(Long currentUserId, Long reservationId) {
        log.info("[取消预约] userId={}, reservationId={}", currentUserId, reservationId);

        Reservation reservation = reservationMapper.selectByReservationIdAndUserId(reservationId, currentUserId);
        if (reservation == null) {
            log.warn("[取消失败] 预约不存在 reservationId={}", reservationId);
            throw new BusinessException(ResultCode.NOT_FOUND, "预约不存在");
        }
        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())) {
            log.warn("[取消失败] 预约已取消 reservationId={}", reservationId);
            throw new BusinessException(ResultCode.BAD_REQUEST, "该预约已经取消过了");
        }
        if (reservation.getEndTime().isBefore(LocalDateTime.now())) {
            log.warn("[取消失败] 预约已过期 reservationId={}", reservationId);
            throw new BusinessException(ResultCode.BAD_REQUEST, "历史预约不能取消");
        }

        int updatedRows = reservationMapper.cancelReservation(reservationId);
        if (updatedRows != 1) {
            log.warn("[取消失败] 预约状态已变化 reservationId={}", reservationId);
            throw new BusinessException(ResultCode.CONFLICT, "预约状态已变化，请刷新后重试");
        }

        log.info("[取消成功] reservationId={}", reservationId);
        return true;
    }

    @Override
    public int expireActiveReservations() {
        return reservationMapper.expireActiveReservations();
    }

    @Override
    public List<ReservationVO> listUserAvailableReservations(Long currentUserId) {
        List<Reservation> reservationList = reservationMapper.selectByUserIdAndStatus(
                currentUserId,
                ReservationStatus.ACTIVE.name()
        );
        return buildReservationVOList(reservationList);
    }

    @Override
    public List<ReservationVO> listUserHistoryReservations(Long currentUserId) {
        List<Reservation> reservationList = reservationMapper.selectByUserIdAndNotStatus(
                currentUserId,
                ReservationStatus.ACTIVE.name()
        );
        return buildReservationVOList(reservationList);
    }

    private Reservation buildSeatReservation(Long currentUserId, SeatReservationCreateDTO request, Long classroomId) {
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.SEAT.name());
        reservation.setResourceId(request.getSeatId());
        reservation.setClassroomId(classroomId);
        reservation.setReserveDate(request.getStartTime().toLocalDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setReason(request.getReason());
        reservation.setStatus(ReservationStatus.ACTIVE.name());
        return reservation;
    }

    private Reservation buildClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request) {
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.CLASSROOM.name());
        reservation.setResourceId(request.getClassroomId());
        reservation.setClassroomId(request.getClassroomId());
        reservation.setReserveDate(request.getStartTime().toLocalDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setReason(request.getReason());
        reservation.setStatus(ReservationStatus.ACTIVE.name());
        return reservation;
    }

    private List<ReservationVO> buildReservationVOList(List<Reservation> reservationList) {
        Set<Long> classroomIds = reservationList.stream()
                .map(Reservation::getClassroomId)
                .collect(Collectors.toSet());

        Set<Long> seatIds = reservationList.stream()
                .filter(reservation -> ResourceType.SEAT.name().equals(reservation.getResourceType()))
                .map(Reservation::getResourceId)
                .collect(Collectors.toSet());

        Map<Long, Classroom> classroomMap = classroomIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectByIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity()));

        Map<Long, Seat> seatMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatMapper.selectByIds(seatIds).stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));

        return reservationList.stream()
                .map(reservation -> reservationToReservationVO(reservation, classroomMap, seatMap))
                .toList();
    }

    private ReservationVO reservationToReservationVO(Reservation reservation,
                                                     Map<Long, Classroom> classroomMap,
                                                     Map<Long, Seat> seatMap) {
        ReservationVO reservationVO = new ReservationVO();
        BeanUtils.copyProperties(reservation, reservationVO);

        Classroom classroom = classroomMap.get(reservation.getClassroomId());
        if (classroom != null) {
            Seat seat = seatMap.get(reservation.getResourceId());
            reservationVO.setResourceName(
                    classroom.getBuilding() + " " + classroom.getRoomNumber() + (seat != null ? " " + seat.getSeatNumber() : "")
            );
        }
        return reservationVO;
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            log.error("预约时间不能为空, start={}, end={}", startTime, endTime);
            throw new BusinessException(ResultCode.BAD_REQUEST, "预约时间不能为空");
        }
        if (endTime.isBefore(startTime)) {
            log.error("预约时间不合法, 结束时间必须晚于开始时间, start={}, end={}", startTime, endTime);
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            log.error("预约时间不合法, 开始时间必须晚于当前时间, start={}, now={}", startTime, LocalDateTime.now());
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须晚于当前时间");
        }
    }

    public Long checkSeatReservableAndGetClassroomId(SeatReservationCreateDTO request) {
        log.info("[座位检查开始] seatId={}, start={}, end={}",
                request.getSeatId(), request.getStartTime(), request.getEndTime());

        Seat seat = seatMapper.selectByIdForUpdate(request.getSeatId());
        log.debug("[加锁座位] seatId={}", request.getSeatId());
        if (seat == null) {
            log.error("座位不存在 seatId={}", request.getSeatId());
            throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
        }
        if (SeatStatus.DISABLED.name().equals(seat.getStatus())) {
            log.error("座位不可用 seatId={}", request.getSeatId());
            throw new BusinessException(ResultCode.FORBIDDEN, "该座位不可用");
        }

        Long classroomId = seat.getClassroomId();
        log.debug("[加锁教室] classroomId={}", classroomId);
        Classroom classroom = classroomMapper.selectByIdForUpdate(classroomId);
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "教室不存在");
        }
        if (!ClassroomStatus.ENABLED.name().equals(classroom.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "教室不可预约");
        }

        List<Reservation> seatConflicts = reservationMapper.selectSeatConflictsForUpdate(
                request.getSeatId(),
                request.getStartTime(),
                request.getEndTime()
        );
        if (!seatConflicts.isEmpty()) {
            log.warn("[座位冲突] seatId={}, start={}, end={}",
                    request.getSeatId(), request.getStartTime(), request.getEndTime());
            throw new BusinessException(ResultCode.CONFLICT, "该座位在当前时间段已被预约");
        }

        boolean withClassroomConflict = !reservationMapper.selectClassroomConflictsForUpdate(
                classroomId,
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withClassroomConflict) {
            log.warn("[教室冲突] classroomId={}, start={}, end={}",
                    classroomId, request.getStartTime(), request.getEndTime());
            throw new BusinessException(ResultCode.CONFLICT, "该教室在当前时间段已被预约");
        }

        return classroomId;
    }

    private void checkClassroomReservable(ClassroomReservationCreateDTO request) {
        log.info("[教室预约检查] classroomId={}, start={}, end={}",
                request.getClassroomId(), request.getStartTime(), request.getEndTime());

        Classroom classroom = classroomMapper.selectByIdForUpdate(request.getClassroomId());
        if (classroom == null) {
            log.error("教室不存在 classroomId={}", request.getClassroomId());
            throw new BusinessException(ResultCode.NOT_FOUND, "教室不存在");
        }
        if (ClassroomStatus.DISABLED.name().equals(classroom.getStatus())) {
            log.error("教室不可用 classroomId={}", request.getClassroomId());
            throw new BusinessException(ResultCode.FORBIDDEN, "该教室不可用");
        }

        boolean withClassroomConflict = !reservationMapper.selectClassroomConflictsForUpdate(
                request.getClassroomId(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withClassroomConflict) {
            log.warn("[教室冲突] classroomId={}", request.getClassroomId());
            throw new BusinessException(ResultCode.CONFLICT, "该教室在当前时间段已被预约");
        }

        boolean withSeatConflict = !reservationMapper.selectSeatConflictsInClassroomForUpdate(
                request.getClassroomId(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withSeatConflict) {
            log.warn("[教室内已有座位预约冲突] classroomId={}", request.getClassroomId());
            throw new BusinessException(ResultCode.CONFLICT, "该教室当前时间段已有座位被预约，不能整间预约");
        }
    }
}
