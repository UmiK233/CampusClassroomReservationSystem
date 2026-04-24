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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private static final ZoneOffset BEIJING_OFFSET = ZoneOffset.ofHours(8);

    private final ReservationMapper reservationMapper;
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    @Transactional
    public Long createSeatReservation(Long currentUserId, SeatReservationCreateDTO request) {
        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = request.getEndTime();
        LocalDateTime utcStartTime = toUtcLocalDateTime(startTime);
        LocalDateTime utcEndTime = toUtcLocalDateTime(endTime);

        validateTime(startTime, endTime);
        Long classroomId = checkSeatReservableAndGetClassroomId(currentUserId, request, utcStartTime, utcEndTime);
        tryConsumeDailyUsageQuota(currentUserId, startTime, endTime);

        Reservation reservation = buildSeatReservation(currentUserId, request, classroomId, utcStartTime, utcEndTime);
        reservationMapper.insert(reservation);
        log.info("[seat reservation created] userId={}, reservationId={}, seatId={}",
                currentUserId, reservation.getId(), request.getSeatId());
        return reservation.getId();
    }

    @Override
    @Transactional
    public Long createClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request) {
        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = request.getEndTime();
        LocalDateTime utcStartTime = toUtcLocalDateTime(startTime);
        LocalDateTime utcEndTime = toUtcLocalDateTime(endTime);

        validateTime(startTime, endTime);
        checkClassroomReservable(request, utcStartTime, utcEndTime);
        tryConsumeDailyUsageQuota(currentUserId, startTime, endTime);

        Reservation reservation = buildClassroomReservation(currentUserId, request, utcStartTime, utcEndTime);
        reservationMapper.insert(reservation);
        log.info("[classroom reservation created] userId={}, reservationId={}, classroomId={}",
                currentUserId, reservation.getId(), request.getClassroomId());
        return reservation.getId();
    }

    @Override
    @Transactional
    public Boolean cancelReservation(Long currentUserId, Long reservationId) {
        Reservation reservation = reservationMapper.selectByReservationIdAndUserId(reservationId, currentUserId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "reservation not found");
        }
        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "reservation already cancelled");
        }
        if (reservation.getEndTime().isBefore(utcNow())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "historical reservation cannot be cancelled");
        }

        int cancelReservationRows = reservationMapper.cancelReservation(reservationId);
        if (cancelReservationRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "reservation status changed, please refresh and retry");
        }

        Long minusMinutes = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        int minusUsageRows = reservationMapper.minusUsage(reservation.getUserId(), reservation.getReserveDate(), minusMinutes);
        if (minusUsageRows != 1) {
            log.error("[cancel reservation rollback failed] userId={}, date={}, minusMinutes={}",
                    reservation.getUserId(), reservation.getReserveDate(), minusMinutes);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "cancel reservation failed");
        }

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

    @Override
    public List<Long> listReservedSeatIds(Long classroomId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LocalDateTime utcStartTime = toUtcLocalDateTime(startTime);
        LocalDateTime utcEndTime = toUtcLocalDateTime(endTime);

        validateTime(startTime, endTime);

        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "classroom not found");
        }
        if (!ClassroomStatus.ENABLED.name().equals(classroom.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "classroom unavailable");
        }

        if (reservationMapper.countClassroomConflicts(classroomId, utcStartTime, utcEndTime) > 0) {
            return seatMapper.selectByClassroomId(classroomId)
                    .stream()
                    .map(Seat::getId)
                    .toList();
        }

        return reservationMapper.selectReservedSeatIdsInClassroom(classroomId, utcStartTime, utcEndTime);
    }

    private Reservation buildSeatReservation(Long currentUserId, SeatReservationCreateDTO request, Long classroomId,
                                             LocalDateTime utcStartTime, LocalDateTime utcEndTime) {
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.SEAT.name());
        reservation.setResourceId(request.getSeatId());
        reservation.setClassroomId(classroomId);
        reservation.setReserveDate(toBeijingDate(request.getStartTime()));
        reservation.setStartTime(utcStartTime);
        reservation.setEndTime(utcEndTime);
        reservation.setReason(request.getReason());
        reservation.setStatus(ReservationStatus.ACTIVE.name());
        return reservation;
    }

    private Reservation buildClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request,
                                                  LocalDateTime utcStartTime, LocalDateTime utcEndTime) {
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setResourceType(ResourceType.CLASSROOM.name());
        reservation.setResourceId(request.getClassroomId());
        reservation.setClassroomId(request.getClassroomId());
        reservation.setReserveDate(toBeijingDate(request.getStartTime()));
        reservation.setStartTime(utcStartTime);
        reservation.setEndTime(utcEndTime);
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

    private void validateTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        validateBasicTime(startTime, endTime);
        validateDurationTime(startTime, endTime);
    }

    private void validateBasicTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "reservation time is required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "end time must be after start time");
        }
        if (startTime.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "start time must be in the future");
        }
        if (!toBeijingDate(startTime).equals(toBeijingDate(endTime))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "reservation cannot cross Beijing date");
        }
    }

    private void validateDurationTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes > 3 * 60) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "single reservation cannot exceed 3 hours");
        }
    }

    public void tryConsumeDailyUsageQuota(Long currentUserId, OffsetDateTime startTime, OffsetDateTime endTime) {
        LocalDate date = toBeijingDate(startTime);
        long addMinutes = Duration.between(startTime, endTime).toMinutes();

        reservationMapper.initUsage(currentUserId, date);
        int updatedRows = reservationMapper.tryAddUsage(currentUserId, date, addMinutes);

        if (updatedRows == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "daily reservation time limit exceeded");
        }
    }

    public Long checkSeatReservableAndGetClassroomId(Long currentUserId, SeatReservationCreateDTO request,
                                                     LocalDateTime utcStartTime, LocalDateTime utcEndTime) {
        Seat seat = seatMapper.selectByIdForUpdate(request.getSeatId());
        if (seat == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "seat not found");
        }
        if (SeatStatus.DISABLED.name().equals(seat.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "seat unavailable");
        }

        Long classroomId = seat.getClassroomId();
        Classroom classroom = classroomMapper.selectByIdForUpdate(classroomId);
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "classroom not found");
        }
        if (!ClassroomStatus.ENABLED.name().equals(classroom.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "classroom unavailable");
        }

        List<Reservation> seatConflicts = reservationMapper.selectSeatConflictsForUpdate(
                request.getSeatId(),
                utcStartTime,
                utcEndTime
        );
        if (!seatConflicts.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "seat is already reserved in this time range");
        }

        boolean withClassroomConflict = !reservationMapper.selectClassroomConflictsForUpdate(
                classroomId,
                utcStartTime,
                utcEndTime
        ).isEmpty();
        if (withClassroomConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "classroom is already reserved in this time range");
        }

        int conflictRows = reservationMapper.selectStudentTimeConflictForUpdate(
                currentUserId,
                utcStartTime,
                utcEndTime
        );
        if (conflictRows >= 1) {
            throw new BusinessException(ResultCode.CONFLICT, "only one reservation is allowed in the same time range");
        }

        return classroomId;
    }

    private void checkClassroomReservable(ClassroomReservationCreateDTO request,
                                          LocalDateTime utcStartTime, LocalDateTime utcEndTime) {
        Classroom classroom = classroomMapper.selectByIdForUpdate(request.getClassroomId());
        if (classroom == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "classroom not found");
        }
        if (ClassroomStatus.DISABLED.name().equals(classroom.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "classroom unavailable");
        }

        boolean withClassroomConflict = !reservationMapper.selectClassroomConflictsForUpdate(
                request.getClassroomId(),
                utcStartTime,
                utcEndTime
        ).isEmpty();
        if (withClassroomConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "classroom is already reserved in this time range");
        }

        boolean withSeatConflict = !reservationMapper.selectSeatConflictsInClassroomForUpdate(
                request.getClassroomId(),
                utcStartTime,
                utcEndTime
        ).isEmpty();
        if (withSeatConflict) {
            throw new BusinessException(ResultCode.CONFLICT, "seats in this classroom are already reserved in this time range");
        }
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDate toBeijingDate(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(BEIJING_OFFSET).toLocalDate();
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
