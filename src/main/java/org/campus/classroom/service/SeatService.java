package org.campus.classroom.service;

import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.vo.ClassroomSeatLayoutVO;
import org.campus.classroom.vo.SeatVO;

public interface SeatService {
    void initSeats(Long classroomId);

//    void regenerateSeats(Long classroomId);
    SeatVO getSeatById(Long id);

    ClassroomSeatLayoutVO getSeatLayout(Long classroomId);

    Boolean update(Long id, SeatUpdateDTO request);

    Boolean batchUpdateSeatStatus(Long classroomId, SeatUpdateDTO request);
}
