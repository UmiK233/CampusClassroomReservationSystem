package org.campus.classroom.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.SeatUpdateDTO;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.SeatService;
import org.campus.classroom.vo.SeatVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @PutMapping("/{id}")
    public Result<SeatVO> update(@PathVariable @NotNull Long id,@RequestBody SeatUpdateDTO request) {
        seatService.update(id, request);
        return Result.success("座位信息更新成功",seatService.getSeatById(id));
    }
}
