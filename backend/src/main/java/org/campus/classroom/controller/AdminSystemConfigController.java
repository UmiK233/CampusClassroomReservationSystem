package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.SystemConfigUpdateDTO;
import org.campus.classroom.service.SystemConfigService;
import org.campus.classroom.vo.SystemConfigVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemConfigController {
    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<SystemConfigVO>> list(@RequestParam(required = false) String category) {
        return Result.success("获取配置列表成功", systemConfigService.listConfigs(category));
    }

    @PutMapping("/{key:.+}")
    public Result<SystemConfigVO> update(@PathVariable String key,
                                         @RequestBody @Valid SystemConfigUpdateDTO request) {
        return Result.success("配置更新成功", systemConfigService.updateConfig(key, request.getConfigValue()));
    }
}
