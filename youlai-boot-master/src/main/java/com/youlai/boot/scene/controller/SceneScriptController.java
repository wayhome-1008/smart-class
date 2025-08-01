package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.SceneScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.SceneScriptForm;
import com.youlai.boot.scene.model.query.SceneScriptQuery;
import com.youlai.boot.scene.model.vo.SceneScriptVO;
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
 * 场景脚本前端控制层
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Tag(name = "场景脚本接口")
@RestController
@RequestMapping("/api/v1/scene-script")
@RequiredArgsConstructor
public class SceneScriptController  {

    private final SceneScriptService sceneScriptService;

    @Operation(summary = "场景脚本分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:scene-script:query')")
    public PageResult<SceneScriptVO> getSceneScriptPage(SceneScriptQuery queryParams ) {
        IPage<SceneScriptVO> result = sceneScriptService.getSceneScriptPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增场景脚本")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:scene-script:add')")
    public Result<Void> saveSceneScript(@RequestBody @Valid SceneScriptForm formData ) {
        boolean result = sceneScriptService.saveSceneScript(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取场景脚本表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:scene-script:edit')")
    public Result<SceneScriptForm> getSceneScriptForm(
        @Parameter(description = "场景脚本ID") @PathVariable Long id
    ) {
        SceneScriptForm formData = sceneScriptService.getSceneScriptFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改场景脚本")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:scene-script:edit')")
    public Result<Void> updateSceneScript(
            @Parameter(description = "场景脚本ID") @PathVariable Long id,
            @RequestBody @Validated SceneScriptForm formData
    ) {
        boolean result = sceneScriptService.updateSceneScript(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除场景脚本")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:scene-script:delete')")
    public Result<Void> deleteSceneScripts(
        @Parameter(description = "场景脚本ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = sceneScriptService.deleteSceneScripts(ids);
        return Result.judge(result);
    }
}
