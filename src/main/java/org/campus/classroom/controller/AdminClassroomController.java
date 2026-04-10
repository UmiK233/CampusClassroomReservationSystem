package org.campus.classroom.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Not;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ClassroomCreateDTO;
import org.campus.classroom.dto.ClassroomUpdateDTO;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/classrooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
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

    // 教室座位初始化
    @PostMapping("/{id}/seats/init")
    public Result<Void> initSeats(@PathVariable Long id) {
        seatService.initSeats(id);
        return Result.success("座位初始化成功");
    }


    //批量更新一个教室下的座位状态
    @PutMapping("/{id}/seats/status")
    public Result<Void> batchUpdateSeatsStatus(@PathVariable Long id,@RequestBody @Valid SeatUpdateDTO request) {
        seatService.batchUpdateSeatStatus(id, request);
        return Result.success("座位状态更新成功");
    }

    @GetMapping("/list")
    public Result<List<ClassroomVO>>  list(@NotNull String building,@NotNull @RequestParam("min_capacity") Integer minCapacity,@NotNull String status) {
        return Result.success("获取成功",classroomService.adminGetClassroomList(building, minCapacity, status));
    }

}
