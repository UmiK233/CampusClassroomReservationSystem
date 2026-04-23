package org.campus.classroom.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ClassroomCreateDTO;
import org.campus.classroom.dto.ClassroomUpdateDTO;
import org.campus.classroom.dto.SeatCreateDTO;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.ClassroomVO;
import org.campus.classroom.vo.SeatVO;
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
        return Result.success("教室创建成功", classroomService.getClassroomById(id));
    }

    @PutMapping("/{id}")
    public Result<ClassroomVO> update(@PathVariable Long id, @RequestBody @Valid ClassroomUpdateDTO request) {
        classroomService.update(id, request);
        return Result.success("教室信息更新成功", classroomService.getClassroomById(id));
    }

    @PostMapping("/{id}/seat_layout")
    public Result<Void> createSeatLayout(@PathVariable Long id) {
        seatService.initSeats(id);
        return Result.success("座位布局初始化成功，已自动补齐缺失座位");
    }

    @PostMapping("/{id}/seats")
    public Result<SeatVO> createSeat(@PathVariable Long id, @RequestBody @Valid SeatCreateDTO request) {
        return Result.success("座位创建成功", seatService.create(id, request));
    }

    @PatchMapping("/{id}/seat_layout")
    public Result<Void> batchUpdateSeats(@PathVariable Long id, @RequestBody @Valid SeatUpdateDTO request) {
        seatService.batchUpdateSeats(id, request);
        return Result.success("座位状态更新成功");
    }

    @GetMapping("/list")
    public Result<List<ClassroomVO>> list(@NotNull String building,
                                          @NotNull @RequestParam("min_capacity") Integer minCapacity,
                                          @NotNull String status) {
        return Result.success("获取成功", classroomService.adminGetClassroomList(building, minCapacity, status));
    }

    @DeleteMapping("/{id}/seat_layout")
    public Result<Void> batchDeleteSeats(@PathVariable Long id) {
        seatService.batchDeleteSeats(id);
        return Result.success("座位布局删除成功");
    }
}
