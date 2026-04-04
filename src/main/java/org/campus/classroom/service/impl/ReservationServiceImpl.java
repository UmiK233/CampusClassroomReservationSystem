package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.entity.Reservation;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ReservationStatus;
import org.campus.classroom.enums.ResourceType;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.ReservationMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.vo.ReservationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    @Transactional
    public Long createSeatReservation(Long currentUserId, SeatReservationCreateDTO request) {
        validateTime(request.getReserveDate(), request.getStartTime(), request.getEndTime());

        int studentReservationCount = reservationMapper.selectStudentTimeConflict(
                currentUserId,
                request.getReserveDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        if (studentReservationCount >= 1) {
            throw new BusinessException(400, "同一时间段只能有一个预约,请先取消之前的预约");
        }

        Seat seat = seatMapper.selectById(request.getSeatId());
        // 判断座位是否存在和可用
        if (seat == null) {
            throw new BusinessException(400, "座位不存在");
        }
        if (seat.getStatus().equals("DISABLED")) {
            throw new BusinessException(400, "该座位不可用");
        }
        //判断座位所在教室是否可用
        Long classroomId = seat.getClassroomId();
        if (classroomMapper.selectById(classroomId).getStatus().equals("DISABLED")) {
            throw new BusinessException(400, "该座位所在教室不可用");
        }
        //判断座位是否被预约
        boolean seatConflict = !reservationMapper.selectSeatConflicts(
                request.getSeatId(),
                request.getReserveDate(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();

        if (seatConflict) {
            throw new BusinessException(400, "该座位在当前时间段已被预约");
        }
        //判断座位所在教室是否被预约
        boolean classroomConflict = !reservationMapper.selectClassroomConflicts(
                classroomId,
                request.getReserveDate(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (classroomConflict) {
            throw new BusinessException(400, "该教室在当前时间段已被整间预约");
        }
        //组装Reservation对象
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.SEAT.name());
        reservation.setResourceId(request.getSeatId());
        reservation.setClassroomId(classroomId);
        reservation.setReserveDate(request.getReserveDate());
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
        validateTime(request.getReserveDate(), request.getStartTime(), request.getEndTime());
        if (classroomMapper.selectById(request.getClassroomId()).getStatus().equals("DISABLED")) {
            throw new BusinessException(400, "该教室不可用");
        }
        boolean classroomConflict = !reservationMapper.selectClassroomConflicts(
                request.getClassroomId(),
                request.getReserveDate(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (classroomConflict) {
            throw new BusinessException(400, "该教室在当前时间段已被预约");
        }

        boolean seatConflict = !reservationMapper.selectSeatConflictsInClassroom(
                request.getClassroomId(),
                request.getReserveDate(),
                request.getStartTime(),
                request.getEndTime()
        ).isEmpty();
        if (seatConflict) {
            throw new BusinessException(400, "该教室当前时间段已有座位被预约，不能整间预约");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.CLASSROOM.name());
        reservation.setResourceId(request.getClassroomId());
        reservation.setClassroomId(request.getClassroomId());
        reservation.setReserveDate(request.getReserveDate());
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
            throw new BusinessException(404, "预约不存在");
        }
        //预约已取消
        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())) {
            throw new BusinessException(400, "该预约已取消");
        }

        //预约已结束
        if (reservation.getReserveDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "历史预约不能取消");
        }
        reservationMapper.updateStatusById(reservationId, ReservationStatus.CANCELLED.name());

        return true;
    }

    @Override
    public List<ReservationVO> listUserReservations(Long currentUserId) {
        List<Reservation> reservationList = reservationMapper.selectByUserId(currentUserId);
        return reservationList.stream().map(item -> {
            ReservationVO reservationVO = new ReservationVO();
            BeanUtils.copyProperties(item, reservationVO);
            return reservationVO;
        }).toList();
    }

    private void validateTime(LocalDate reserveDate, LocalTime startTime, LocalTime endTime) {
        if (reserveDate == null) {
            throw new BusinessException(404, "预约日期不能为空");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException(404, "预约时间不能为空");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
        if (reserveDate.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "不能预约过去日期");
        }
        if (reserveDate.isEqual(LocalDate.now()) && !startTime.isAfter(LocalTime.now())) {
            throw new BusinessException(400, "开始时间必须晚于当前时间");
        }
    }
}
