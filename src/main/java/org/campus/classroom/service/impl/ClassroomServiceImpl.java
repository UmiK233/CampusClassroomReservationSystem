package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.dto.ClassroomCreateDTO;
import org.campus.classroom.dto.ClassroomUpdateDTO;
import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ClassroomStatus;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.enums.SeatStatus;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.vo.ClassroomVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomServiceImpl implements ClassroomService {
    private final ClassroomMapper classroomMapper;
    private final SeatMapper seatMapper;

    @Transactional
    @Override
    public Long create(ClassroomCreateDTO request) {
        log.info("[教室创建开始] building={}, roomNumber={}, seatRows={}, seatCols={}",
                request.getBuilding(), request.getRoomNumber(), request.getSeatRows(), request.getSeatCols());
        //1.判重
        Classroom existing = classroomMapper.selectByBuildingAndNumber(request.getBuilding().trim(), request.getRoomNumber().trim());
        if (existing != null) {
            log.warn("[教室创建失败] building={}, roomNumber={}, reason=duplicate classroom",
                    request.getBuilding(), request.getRoomNumber());
            throw new BusinessException(ResultCode.CONFLICT, "该教学楼下教室名称已存在");
        }

        //2. 检验DTO的status参数, 若不存在则默认设为ENABLE, 若存在则判断是否合法后使用
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = ClassroomStatus.ENABLED.name();
        } else {
            status = status.trim();
            validateStatus(status);
        }

        //接收前端传来的DTO对象并将其转为后端所需的Entity对象
        Classroom classroom = new Classroom();
        classroom.setRoomNumber(request.getRoomNumber());
        classroom.setBuilding(request.getBuilding());
        classroom.setSeatRows(request.getSeatRows());
        classroom.setSeatCols(request.getSeatCols());
        classroom.setStatus(status);
        classroom.setRemark(request.getRemark());

        //将Entity对象插入数据库
        int rows = classroomMapper.insert(classroom);
        if (rows <= 0) {
            log.error("[教室创建失败] building={}, roomNumber={}, reason=insert failed",
                    request.getBuilding(), request.getRoomNumber());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建失败");
        }
        //插入成功后，MyBatis会自动将生成的主键ID设置到classroom对象的id属性中,因此我们可以直接使用classroom.getId()来获取新创建的教室ID。
        //根据教室的行列数生成座位数据并插入数据库
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
        log.info("[教室创建成功] classroomId={}, building={}, roomNumber={}",
                classroom.getId(), classroom.getBuilding(), classroom.getRoomNumber());
        return classroom.getId();
    }

    /**
     * 更新教室信息，支持更新状态Status和备注信息Remark
     * @param id:要更新的教室ID
     * @param request:ClassroomUpdateDTO类,包含status和remark两个属性,其中status是可选的,如果不传则默认不修改状态,如果传了则必须是合法的状态值
     * @return Boolean:更新成功返回true,更新失败抛出异常
     */
    @Override
    public Boolean update(Long id, ClassroomUpdateDTO request) {
        log.info("[教室更新开始] classroomId={}, request={}", id, request);
        //1. 查询是否存在
        Classroom existing = classroomMapper.selectById(id);
        if (existing == null) {
            log.warn("[教室更新失败] classroomId={}, reason=not found", id);
            throw new BusinessException(ResultCode.BAD_REQUEST, "教室不存在");
        }
        //2. 是否重复教室
        checkDuplicate(existing.getBuilding().trim(), existing.getRoomNumber().trim(), id);

        //3. 检验DTO的status参数,若不存在则默认为之前的,若存在则判断是否合法后使用
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            status = existing.getStatus();
        } else {
            status = status.trim();
            validateStatus(status);
        }

        //将新的DTO对象转换为Entity对象后更新到数据库
        Classroom classroom = new Classroom();
        classroom.setId(id);
        classroom.setStatus(status);
        classroom.setRemark(request.getRemark());

        int rows = classroomMapper.updateById(classroom);
        if (rows <= 0) {
            log.error("[教室更新失败] classroomId={}, reason=update failed", id);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "更新失败");
        }
        log.info("[教室更新成功] classroomId={}, status={}", id, status);
        return true;
    }

    @Override
    public ClassroomVO getClassroomById(Long id) {
        log.info("[教室查询] classroomId={}", id);
        //1.查询是否存在
        Classroom classroom = classroomMapper.selectById(id);
        if (classroom == null) {
            log.warn("[教室查询失败] classroomId={}, reason=not found", id);
            throw new BusinessException(ResultCode.BAD_REQUEST,"教室不存在");
        }
        //2.将Entity对象转换为VO对象
        ClassroomVO classroomVO = classroomToClassroomVO(classroom);
        log.info("[教室查询成功] classroomId={}", id);
        return classroomVO;
    }

    /**
     * 查询可用的教室列表，支持根据教学楼和最小容量进行过滤
     *
     * @param building
     * @param minCapacity
     * @return
     */
    @Override
    public List<ClassroomVO> getAvailableClassroomList(String building, Integer minCapacity) {
        log.info("[教室可用列表查询] building={}, minCapacity={}", building, minCapacity);
        //判断教室是否可用
        List<Classroom> classroomList = classroomMapper.selectList(building, minCapacity, ClassroomStatus.ENABLED.name());
        //将Entity对象转换为VO对象
        List<ClassroomVO> classroomVOS = classroomList.stream()
                .map(this::classroomToClassroomVO)
                .collect(Collectors.toList());
        log.info("[教室可用列表结果] count={}", classroomVOS.size());
        return classroomVOS;
    }

    /**
     * 管理员查询教室列表，支持根据教学楼、最小容量和状态进行过滤
     *
     * @param building
     * @param minCapacity
     * @param status
     * @return
     */
    @Override
    public List<ClassroomVO> adminGetClassroomList(String building, Integer minCapacity, String status) {
        log.info("[管理员教室列表查询] building={}, minCapacity={}, status={}", building, minCapacity, status);
        //校验教室状态是否合法
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }
        List<Classroom> classrooms = classroomMapper.selectList(building, minCapacity, status);
        List<ClassroomVO> classroomVOS = classrooms.stream()
                .map(this::classroomToClassroomVO)
                .toList();
        log.info("[管理员教室列表查询结果] count={}", classroomVOS.size());
        return classroomVOS;
    }

    private void checkDuplicate(String building, String roomNumber, Long currentId) {
        Classroom duplicate = classroomMapper.selectByBuildingAndNumber(building, roomNumber);
        //如果没有查询到重复的教室，说明没有重复问题，直接返回
        if (duplicate == null) {
            return;
        }
        //如果查询到的重复教室的ID和当前正在更新的教室ID不同，说明存在重复问题，抛出异常
        if (!duplicate.getId().equals(currentId)) {
            log.warn("[教室重复] building={}, roomNumber={}, currentId={}, duplicateId={}",
                    building, roomNumber, currentId, duplicate.getId());
            throw new BusinessException(ResultCode.BAD_REQUEST, "该教学楼下教室名称已存在");
        }
    }

    private void validateStatus(String status) {
        if (!ClassroomStatus.ENABLED.name().equals(status)
                && !ClassroomStatus.DISABLED.name().equals(status)) {
            log.warn("[教室状态非法] status={}", status);
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 ENABLED 或 DISABLED");
        }
    }

    private ClassroomVO classroomToClassroomVO(Classroom classroom) {
        ClassroomVO classroomVO = new ClassroomVO();
        BeanUtils.copyProperties(classroom, classroomVO);
        classroomVO.setCapacity(classroom.getSeatRows() * classroom.getSeatCols());
        return classroomVO;
    }

}
