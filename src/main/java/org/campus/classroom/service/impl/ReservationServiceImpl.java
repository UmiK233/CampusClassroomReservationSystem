package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Reservation;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.*;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.ReservationMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.vo.ClassroomVO;
import org.campus.classroom.vo.ReservationVO;
import org.campus.classroom.vo.SeatVO;
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
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    @Transactional
    public Long createSeatReservation(Long currentUserId, SeatReservationCreateDTO request) {
        validateTime(request.getStartTime(), request.getEndTime());

        int studentReservationCount = reservationMapper.selectStudentTimeConflict(
                currentUserId,
                request.getStartTime(),
                request.getEndTime()
        );
        if (studentReservationCount >= 1) {
            throw new BusinessException(ResultCode.CONFLICT, "同一时间段只能有一个预约,请先取消之前的预约");
        }

        Long classroomId = checkSeatReservableAndGetClassroomId(request);


        //组装Reservation对象
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

        reservationMapper.insert(reservation);

        return reservation.getId();
    }

    @Override
    @Transactional
    public Long createClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request) {
        validateTime(request.getStartTime(), request.getEndTime());

        checkClassroomReservable(request);

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

        reservationMapper.insert(reservation);
        return reservation.getId();
    }

    @Override
    public Boolean cancelReservation(Long currentUserId, Long reservationId) {
        Reservation reservation = reservationMapper.selectByIdAndUserId(reservationId, currentUserId);
        //预约不存在
        if (reservation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "预约不存在");
        }
        //预约已取消
        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该预约已经取消过了");
        }

        //预约已结束
        if (reservation.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "历史预约不能取消");
        }
        reservationMapper.cancelReservation(reservationId);

        return true;
    }

    @Override
    public int expireActiveReservations() {
        return reservationMapper.expireActiveReservations();
    }

    @Override
    public List<ReservationVO> listUserReservations(Long currentUserId) {
        List<Reservation> reservationList = reservationMapper.selectByUserId(currentUserId);

        // 1. 收集所有 classroomId
        Set<Long> classroomIds = reservationList.stream()
                .map(Reservation::getClassroomId)
                .collect(Collectors.toSet());

        // 2. 收集所有 seatId（只有座位预约时收集）
        Set<Long> seatIds = reservationList.stream()
                .filter(reservation -> ResourceType.SEAT.name().equals(reservation.getResourceType()))
                .map(Reservation::getResourceId)
                .collect(Collectors.toSet());

        // 3. 批量查 classroom
        Map<Long, ClassroomVO> classroomVOMap = classroomIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectByIds(classroomIds).stream().map(this::classroomToClassroomVO)
                .collect(Collectors.toMap(ClassroomVO::getId, Function.identity()));

        // 4. 批量查 seat
        Map<Long, SeatVO> seatVOMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatMapper.selectByIds(seatIds).stream().map(this::seatToSeatVO)
                .collect(Collectors.toMap(SeatVO::getId, Function.identity()));


        return reservationList.stream().map(reservation -> reservationToReservationVO(reservation, classroomVOMap, seatVOMap)).toList();

    }

    private ClassroomVO classroomToClassroomVO(Classroom classroom) {
        ClassroomVO classroomVO = new ClassroomVO();
        BeanUtils.copyProperties(classroom, classroomVO);
        classroomVO.setCapacity(classroom.getSeatRows() * classroom.getSeatCols());
        return classroomVO;
    }

    private SeatVO seatToSeatVO(Seat seat) {
        SeatVO seatVO = new SeatVO();
        BeanUtils.copyProperties(seat, seatVO);
        return seatVO;
    }

    private ReservationVO reservationToReservationVO(Reservation reservation, Map<Long, ClassroomVO> classroomVOMap,
                                                     Map<Long, SeatVO> seatVOMap) {
        ReservationVO reservationVO = new ReservationVO();
        BeanUtils.copyProperties(reservation, reservationVO);
        ClassroomVO classroomVO = classroomVOMap.get(reservation.getClassroomId());
        if (classroomVO != null) {
            reservationVO.setClassroomVO(classroomVO);
        }

        if (reservation.getResourceType().equals(ResourceType.SEAT.name())) {
            SeatVO seatVO = seatVOMap.get(reservation.getResourceId());
            if (seatVO != null) {
                reservationVO.setSeatVO(seatVO);
            }
        }

        return reservationVO;
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
//        if (reserveDate == null) {
//            throw new BusinessException(ResultCode.BAD_REQUEST, "预约日期不能为空");
//        }
        if (startTime == null || endTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预约时间不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "开始时间必须晚于当前时间");
        }

    }


    private Long checkSeatReservableAndGetClassroomId(SeatReservationCreateDTO request) {
        Seat seat = seatMapper.selectById(request.getSeatId());
        // 判断座位是否存在和可用
        if (seat == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
        }
        if (seat.getStatus().equals(SeatStatus.DISABLED.name())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该座位不可用");
        }
        //判断座位所在教室是否可用
        Long classroomId = seat.getClassroomId();
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "教室不存在");
        }
        if (classroom.getStatus().equals(ClassroomStatus.DISABLED.name())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该座位所在教室不可用");
        }
        //判断座位是否被预约
        boolean withSeatConflict = !reservationMapper.selectSeatConflicts(
                request.getSeatId(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();

        if (withSeatConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "该座位在当前时间段已被预约");
        }
        //判断座位所在教室是否被预约
        boolean withClassroomConflict = !reservationMapper.selectClassroomConflicts(
                classroomId,
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withClassroomConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "该教室在当前时间段已被预约");
        }
        return classroomId;
    }

    private void checkClassroomReservable(ClassroomReservationCreateDTO request) {
        Classroom classroom = classroomMapper.selectById(request.getClassroomId());
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "教室不存在");
        }
        if (classroom.getStatus().equals(SeatStatus.DISABLED.name())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该教室不可用");
        }
        boolean withClassroomConflict = !reservationMapper.selectClassroomConflicts(
                request.getClassroomId(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withClassroomConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "该教室在当前时间段已被预约");
        }

        boolean withSeatConflict = !reservationMapper.selectSeatConflictsInClassroom(
                request.getClassroomId(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (withSeatConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "该教室当前时间段已有座位被预约，不能整间预约");
        }
    }


}
