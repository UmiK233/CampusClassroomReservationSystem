package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ClassroomStatus;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.enums.SeatStatus;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomSeatLayoutVO;
import org.campus.classroom.vo.SeatVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatServiceImpl implements SeatService {
    private final SeatMapper seatMapper;
    private final ClassroomMapper classroomMapper;

    @Override
    public void initSeats(Long classroomId) {
        log.info("[座位初始化开始] classroomId={}", classroomId);
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.判断教室是否已经初始化过座位
        int count = seatMapper.countByClassroomId(classroomId);
        if (count > 0) {
            log.warn("[座位初始化失败] classroomId={}, reason=already initialized, seatCount={}", classroomId, count);
            throw new BusinessException(ResultCode.BAD_REQUEST, "座位已经初始化过了");
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
        log.info("[座位初始化成功] classroomId={}, seatCount={}",
                classroomId, classroom.getSeatRows() * classroom.getSeatCols());
    }

    @Override
    public SeatVO getSeatById(Long id) {
        log.info("[座位查询] seatId={}", id);
        Seat seat = seatMapper.selectById(id);
        if (seat == null) {
            log.warn("[座位查询失败] seatId={}, reason=not found", id);
            throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
        }
        SeatVO seatVO = seatToSeatVO(seat);
        log.info("[座位查询成功] seatId={}", id);
        return seatVO;
    }

    @Override
    public ClassroomSeatLayoutVO getSeatLayout(Long classroomId) {
        log.info("[教室座位布局查询开始] classroomId={}", classroomId);
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.查询座位列表并转换为VO
        List<SeatVO> seatVOS = seatMapper.selectByClassroomId(classroomId).stream().map(this::seatToSeatVO).toList();
        Classroom classroom = classroomMapper.selectById(classroomId);
        ClassroomSeatLayoutVO classroomSeatLayoutVO = new ClassroomSeatLayoutVO();
        classroomSeatLayoutVO.setClassroomId(classroomId);
        classroomSeatLayoutVO.setClassroomBuilding(classroom.getBuilding());
        classroomSeatLayoutVO.setClassroomNumber(classroom.getRoomNumber());
        classroomSeatLayoutVO.setSeatCols(classroom.getSeatCols());
        classroomSeatLayoutVO.setSeatRows(classroom.getSeatRows());
        classroomSeatLayoutVO.setSeatVOS(seatVOS);
        log.info("[教室座位布局查询成功] classroomId={}, seatCount={}", classroomId, seatVOS.size());
        return classroomSeatLayoutVO;
    }

    @Override
    public Boolean update(Long id, SeatUpdateDTO request) {
        log.info("[座位更新开始] seatId={}, request={}", id, request);
        //1.查询座位是否存在
        Seat existing = seatMapper.selectById(id);
        if (existing == null) {
            log.warn("[座位更新失败] seatId={}, reason=not found", id);
            throw new BusinessException(ResultCode.NOT_FOUND, "座位不存在");
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
        log.info("[座位更新成功] seatId={}, status={}", id, status);
        return true;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean batchUpdateSeatStatus(Long classroomId, SeatUpdateDTO request) {
        log.info("[座位批量更新开始] classroomId={}, request={}", classroomId, request);
        //1.验证教室是否存在且可用
        validateClassroom(classroomId);
        //2.检验status参数,若不存在则直接抛出错误,因为该接口就是用来批量更新状态的,若存在则判断是否合法后使用
        String status = request.getStatus();

        if (status == null || status.trim().isEmpty()) {
            log.warn("[座位批量更新失败] classroomId={}, reason=empty status", classroomId);
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态不能为空");
        }
        status = status.trim();
        validateStatus(status);
        //3.批量更新座位状态
        int rows = seatMapper.batchUpdateByClassroomId(classroomId, status, request.getRemark());
        if (rows <= 0) {
            log.error("[座位批量更新失败] classroomId={}, status={}, reason=no seat updated", classroomId, status);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "未更新到任何座位");
        }
        log.info("[座位批量更新成功] classroomId={}, status={}, updatedRows={}", classroomId, status, rows);
        return true;

    }

    private void validateStatus(String status) {
        if (!SeatStatus.ENABLED.name().equals(status)
                && !SeatStatus.DISABLED.name().equals(status)) {
            log.warn("[座位状态非法] status={}", status);
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 ENABLED 或 DISABLED");
        }
    }

    private void validateClassroom(Long classroomId) {
        //1.查询教室是否存在
        Classroom classroom = classroomMapper.selectById(classroomId);
        if (classroom == null) {
            log.warn("[教室校验失败] classroomId={}, reason=not found", classroomId);
            throw new BusinessException(ResultCode.NOT_FOUND, "教室不存在");
        }
        //2.查询教室是否可用
        if (!ClassroomStatus.ENABLED.name().equals(classroom.getStatus())) {
            log.warn("[教室校验失败] classroomId={}, reason=disabled, status={}", classroomId, classroom.getStatus());
            throw new BusinessException(ResultCode.FORBIDDEN, "教室不可用");
        }
    }

    private SeatVO seatToSeatVO(Seat seat) {
        SeatVO seatVO = new SeatVO();
        BeanUtils.copyProperties(seat, seatVO);
        return seatVO;
    }
}
