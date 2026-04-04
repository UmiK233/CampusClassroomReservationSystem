package org.campus.classroom.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomSeatLayoutVO;
import org.campus.classroom.vo.ClassroomVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classrooms")
@RequiredArgsConstructor
public class ClassroomController {
    private final ClassroomService classroomService;
    private final SeatService seatService;

    @GetMapping("/{id}")
    public Result<ClassroomVO> getClassroomById(@PathVariable @NotNull Long id) {
        ClassroomVO classroomVO = classroomService.getClassroomById(id);
        return Result.success("查询成功", classroomVO);
    }

    @GetMapping("/{id}/seats")
    public Result<ClassroomSeatLayoutVO> getClassroomSeatLayout(@PathVariable @NotNull Long id) {
        ClassroomSeatLayoutVO classroomSeatLayoutVO = seatService.getSeatLayout(id);
        return Result.success("查询成功", classroomSeatLayoutVO);
    }


    @GetMapping("/available_list")
    public Result<List<ClassroomVO>> list(String building, @RequestParam("min_capacity") Integer minCapacity) {
        return Result.success("获取成功", classroomService.getAvailableClassroomList(building, minCapacity));
    }
}
