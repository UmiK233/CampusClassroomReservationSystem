package org.campus.classroom.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ClassroomReservationCreateDTO;
import org.campus.classroom.dto.SeatReservationCreateDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.utils.JwtUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/seat")
    public Result<Long> createSeatReservation(HttpServletRequest httpRequest, @RequestBody @Valid SeatReservationCreateDTO request, @AuthenticationPrincipal LoginUser loginUser) {
        Long reservationId = reservationService.createSeatReservation(loginUser.getId(), request);
        return Result.success("预约成功", reservationId);
    }

    @PostMapping("/classroom")
    public Result<Long> createClassroom(HttpServletRequest httpRequest, @RequestBody @Valid ClassroomReservationCreateDTO request, @AuthenticationPrincipal LoginUser loginUser) {
        Long reservationId = reservationService.createClassroomReservation(loginUser.getId(), request);
        return Result.success("预约成功", reservationId);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelReservation(HttpServletRequest httpRequest, @PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        reservationService.cancelReservation(loginUser.getId(), id);
        return Result.success("取消成功");
    }

}
