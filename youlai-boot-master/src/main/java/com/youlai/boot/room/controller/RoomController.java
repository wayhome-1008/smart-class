package com.youlai.boot.room.controller;

import com.youlai.boot.common.model.Option;
import com.youlai.boot.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.room.model.form.RoomForm;
import com.youlai.boot.room.model.query.RoomQuery;
import com.youlai.boot.room.model.vo.RoomVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.youlai.boot.common.result.PageResult;
import com.youlai.boot.common.result.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 房间管理前端控制层
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Tag(name = "08.房间管理接口")
@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class RoomController  {

    private final RoomService roomService;

    @Operation(summary = "房间管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('room:room:query')")
    public PageResult<RoomVO> getRoomPage(RoomQuery queryParams ) {
        IPage<RoomVO> result = roomService.getRoomPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "房间下拉列表")
    @GetMapping("/options")
    public Result<List<Option<Long>>> listRoomOptions() {
        List<Option<Long>> list = roomService.listRoomOptions();
        return Result.success(list);
    }

    @Operation(summary = "新增房间管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('room:room:add')")
    public Result<Void> saveRoom(@RequestBody @Valid RoomForm formData ) {
        boolean result = roomService.saveRoom(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取房间管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('room:room:edit')")
    public Result<RoomForm> getRoomForm(
        @Parameter(description = "房间管理ID") @PathVariable Long id
    ) {
        RoomForm formData = roomService.getRoomFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改房间管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('room:room:edit')")
    public Result<Void> updateRoom(
            @Parameter(description = "房间管理ID") @PathVariable Long id,
            @RequestBody @Validated RoomForm formData
    ) {
        boolean result = roomService.updateRoom(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除房间管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('room:room:delete')")
    public Result<Void> deleteRooms(
        @Parameter(description = "房间管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = roomService.deleteRooms(ids);
        return Result.judge(result);
    }
}
