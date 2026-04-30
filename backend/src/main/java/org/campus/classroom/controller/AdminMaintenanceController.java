package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.MaintenanceCreateDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.MaintenanceService;
import org.campus.classroom.vo.MaintenanceWindowVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMaintenanceController {
    private final MaintenanceService maintenanceService;

    @GetMapping
    public Result<List<MaintenanceWindowVO>> list(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String resourceType,
                                                  @RequestParam(required = false) Long classroomId) {
        return Result.success("获取维护列表成功", maintenanceService.list(status, resourceType, classroomId));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid MaintenanceCreateDTO request,
                               @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success("创建维护成功", maintenanceService.create(loginUser.getId(), request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        maintenanceService.cancel(id);
        return Result.success("取消维护成功");
    }
}
