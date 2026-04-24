package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.AdminReservationCancelDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AdminService;
import org.campus.classroom.vo.AdminReservationVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {
    private final AdminService adminService;

    @GetMapping
    public Result<List<AdminReservationVO>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        return Result.success("获取预约列表成功", adminService.listReservations(keyword, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestBody(required = false) @Valid AdminReservationCancelDTO request,
                               @AuthenticationPrincipal LoginUser loginUser) {
        adminService.cancelReservation(loginUser.getId(), id, request == null ? null : request.getReason());
        return Result.success("管理员取消预约成功");
    }
}
