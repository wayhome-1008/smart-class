package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.SceneForm;
import com.youlai.boot.scene.model.query.SceneQuery;
import com.youlai.boot.scene.model.vo.SceneVO;
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

/**
 * 场景交互前端控制层
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Tag(name = "场景交互接口")
@RestController
@RequestMapping("/api/v1/scene")
@RequiredArgsConstructor
public class SceneController  {

    private final SceneService sceneService;

    @Operation(summary = "场景交互分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:scene:query')")
    public PageResult<SceneVO> getScenePage(SceneQuery queryParams ) {
        IPage<SceneVO> result = sceneService.getScenePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增场景交互")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:scene:add')")
    public Result<Void> saveScene(@RequestBody @Valid SceneForm formData ) {
        boolean result = sceneService.saveScene(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取场景交互表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:scene:edit')")
    public Result<SceneForm> getSceneForm(
        @Parameter(description = "场景交互ID") @PathVariable Long id
    ) {
        SceneForm formData = sceneService.getSceneFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改场景交互")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:scene:edit')")
    public Result<Void> updateScene(
            @Parameter(description = "场景交互ID") @PathVariable Long id,
            @RequestBody @Validated SceneForm formData
    ) {
        boolean result = sceneService.updateScene(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除场景交互")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:scene:delete')")
    public Result<Void> deleteScenes(
        @Parameter(description = "场景交互ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = sceneService.deleteScenes(ids);
        return Result.judge(result);
    }
}
