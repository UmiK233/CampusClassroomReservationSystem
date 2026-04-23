package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.AdminUserStatusUpdateDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AdminService;
import org.campus.classroom.vo.AdminUserVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminService adminService;

    @GetMapping
    public Result<List<AdminUserVO>> list(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String role,
                                          @RequestParam(required = false) Integer status) {
        return Result.success("获取用户列表成功", adminService.listUsers(keyword, role, status));
    }

    @PatchMapping("/{id}/status")
    public Result<AdminUserVO> updateStatus(@PathVariable Long id,
                                            @RequestBody @Valid AdminUserStatusUpdateDTO request,
                                            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(
                "用户状态更新成功",
                adminService.updateUserStatus(loginUser.getId(), id, request.getStatus(), request.getReason())
        );
    }
}
