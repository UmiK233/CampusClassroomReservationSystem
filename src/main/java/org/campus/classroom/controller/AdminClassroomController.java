package org.campus.classroom.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ClassroomCreateDTO;
import org.campus.classroom.dto.ClassroomUpdateDTO;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/classrooms")
@RequiredArgsConstructor
public class AdminClassroomController {
    private final ClassroomService classroomService;
    private final SeatService seatService;

    @PostMapping
    public Result<ClassroomVO> create(@RequestBody @Valid ClassroomCreateDTO request) {
        Long id = classroomService.create(request);
        ClassroomVO classroomVO = classroomService.getClassroomById(id);
        return Result.success("教室创建成功", classroomVO);
    }

    @PutMapping("/{id}")
    public Result<ClassroomVO> update(@PathVariable Long id,@RequestBody @Valid ClassroomUpdateDTO request) {
        classroomService.update(id, request);
        ClassroomVO classroomVO = classroomService.getClassroomById(id);
        return Result.success("教室信息更新成功", classroomVO);
    }

    @PostMapping("/{id}/seats/init")
    public Result<Void> initSeats(@PathVariable Long id) {
        seatService.initSeats(id);
        return Result.success("座位初始化成功");
    }

    @PutMapping("/{id}/seats/status")
    public Result<Void> batchUpdateSeatsStatus(@PathVariable Long id,@RequestBody @Valid SeatUpdateDTO request) {
        seatService.batchUpdateSeatStatus(id, request);
        return Result.success("座位状态更新成功");
    }

    @GetMapping("/list")
    public List<ClassroomVO> list(String building, @RequestParam("min_capacity") Integer minCapacity, String status) {
        return classroomService.adminGetClassroomList(building, minCapacity, status);
    }

}
