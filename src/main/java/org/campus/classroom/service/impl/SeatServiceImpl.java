package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ClassroomStatus;
import org.campus.classroom.enums.SeatStatus;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomSeatLayoutVO;
import org.campus.classroom.vo.SeatVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    public void initSeats(Long classroomId) {
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.判断教室是否已经初始化过座位
        int count = seatMapper.countByClassroomId(classroomId);
        if (count > 0) {
            throw new BusinessException(400, "座位已经初始化过了");
        }
        Classroom classroom = classroomMapper.selectById(classroomId);
        //初始化座位
        for (int i = 1; i <= classroom.getSeatRows(); i++) {
            for (int j = 1; j <= classroom.getSeatCols(); j++) {
                Seat seat = new Seat();
                seat.setClassroomId(classroom.getId());
                seat.setSeatNumber(i + "-" + j);
                seat.setRowNumber(i);
                seat.setColNumber(j);
                seat.setStatus(SeatStatus.ENABLED.name());
                seatMapper.insert(seat);
            }
        }
    }

    @Override
    public SeatVO getSeatById(Long id) {
        Seat seat = seatMapper.selectById(id);
        if (seat == null) {
            throw new BusinessException(404, "座位不存在");
        }
        return toVO(seat);
    }

    @Override
    public ClassroomSeatLayoutVO getSeatLayout(Long classroomId) {
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.查询座位列表并转换为VO
        List<SeatVO> seatVOS = seatMapper.selectByClassroomId(classroomId).stream().map(this::toVO).toList();
        Classroom classroom = classroomMapper.selectById(classroomId);
        ClassroomSeatLayoutVO classroomSeatLayoutVO = new ClassroomSeatLayoutVO();
        classroomSeatLayoutVO.setClassroomId(classroomId);
        classroomSeatLayoutVO.setClassroomBuilding(classroom.getBuilding());
        classroomSeatLayoutVO.setClassroomNumber(classroom.getRoomNumber());
        classroomSeatLayoutVO.setSeatCols(classroom.getSeatCols());
        classroomSeatLayoutVO.setSeatRows(classroom.getSeatRows());
        classroomSeatLayoutVO.setSeatVOS(seatVOS);
        return classroomSeatLayoutVO;
    }

    @Override
    public Boolean update(Long id, SeatUpdateDTO request) {
        //1.查询座位是否存在
        Seat existing = seatMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "座位不存在");
        }
        //2. 检验DTO的status参数,若不存在则默认为之前的,若存在则判断是否合法后使用
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = existing.getStatus();
        }

        status = status.trim();
        validateStatus(status);

        Seat seat = new Seat();
        seat.setId(id);
        seat.setClassroomId(existing.getClassroomId());
        seat.setSeatNumber(existing.getSeatNumber());
        seat.setRowNumber(existing.getRowNumber());
        seat.setColNumber(existing.getColNumber());
        seat.setStatus(status);
        seat.setRemark(request.getRemark());
        seatMapper.updateById(seat);
        return true;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean batchUpdateSeatStatus(Long classroomId, SeatUpdateDTO request) {
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.检验status参数,若不存在则直接抛出错误,因为该接口就是用来批量更新状态的,若存在则判断是否合法后使用
        String status = request.getStatus();

        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException(400, "状态不能为空");
        }
        status = status.trim();
        validateStatus(status);
        //3.批量更新座位状态
        int rows = seatMapper.batchUpdateByClassroomId(classroomId, status, request.getRemark());
        if (rows <= 0) {
            throw new BusinessException(400, "未更新到任何座位");
        }
        return true;

    }

    private void validateStatus(String status) {
        if (!SeatStatus.ENABLED.name().equals(status)
                && !SeatStatus.DISABLED.name().equals(status)) {
            throw new BusinessException(402, "状态只能是 ENABLED 或 DISABLED");
        }
    }

    private void validateClassroom(Long classroomId) {
        //1.查询教室是否存在
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            throw new BusinessException(404, "教室不存在");
        }
        //2.查询教室是否可用
        if (!ClassroomStatus.ENABLED.name().equals(classroom.getStatus())) {
            throw new BusinessException(400, "教室不可用");
        }
    }

    private SeatVO toVO(Seat seat) {
        SeatVO seatVO = new SeatVO();
        seatVO.setId(seat.getId());
        seatVO.setClassroomId(seat.getClassroomId());
        seatVO.setSeatNumber(seat.getSeatNumber());
        seatVO.setRowNumber(seat.getRowNumber());
        seatVO.setColNumber(seat.getColNumber());
        seatVO.setStatus(seat.getStatus());
        seatVO.setRemark(seat.getRemark());
        return seatVO;
    }
}
