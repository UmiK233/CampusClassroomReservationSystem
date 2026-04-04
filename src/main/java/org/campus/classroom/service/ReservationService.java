package org.campus.classroom.service;

import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.vo.ReservationVO;

import java.util.List;

public interface ReservationService {
    Long createSeatReservation(Long currentUserId, SeatReservationCreateDTO request);

    Long createClassroomReservation(Long currentUserId, ClassroomReservationCreateDTO request);

    Boolean cancelReservation(Long currentUserId,Long reservationId);

    List<ReservationVO> listUserReservations(Long currentUserId);
}
