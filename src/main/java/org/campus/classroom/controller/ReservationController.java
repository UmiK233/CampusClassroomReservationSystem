package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.vo.ReservationVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping("/seat")
    public Result<Long> createSeatReservation(@RequestBody @Valid SeatReservationCreateDTO request,
                                              @AuthenticationPrincipal LoginUser loginUser) {
        Long reservationId = reservationService.createSeatReservation(loginUser.getId(), request);
        return Result.success("预约座位成功", reservationId);
    }

    @PostMapping("/classroom")
    public Result<Long> createClassroom(@RequestBody @Valid ClassroomReservationCreateDTO request,
                                        @AuthenticationPrincipal LoginUser loginUser) {
        Long reservationId = reservationService.createClassroomReservation(loginUser.getId(), request);
        return Result.success("预约教室成功", reservationId);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelReservation(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        reservationService.cancelReservation(loginUser.getId(), id);
        return Result.success("取消预约成功");
    }

    @GetMapping("/list")
    public Result<List<ReservationVO>> listReservations(@AuthenticationPrincipal LoginUser loginUser) {
        List<ReservationVO> reservationVOList = reservationService.listUserReservations(loginUser.getId());
        return Result.success("查询预约列表成功", reservationVOList);
    }
}
