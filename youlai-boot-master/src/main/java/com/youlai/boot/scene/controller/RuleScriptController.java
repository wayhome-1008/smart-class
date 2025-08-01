package com.youlai.boot.scene.controller;

import com.youlai.boot.scene.service.RuleScriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.scene.model.form.RuleScriptForm;
import com.youlai.boot.scene.model.query.RuleScriptQuery;
import com.youlai.boot.scene.model.vo.RuleScriptVO;
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
 * 规则引擎脚本前端控制层
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Tag(name = "规则引擎脚本接口")
@RestController
@RequestMapping("/api/v1/rule-script")
@RequiredArgsConstructor
public class RuleScriptController  {

    private final RuleScriptService ruleScriptService;

    @Operation(summary = "规则引擎脚本分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('scene:rule-script:query')")
    public PageResult<RuleScriptVO> getRuleScriptPage(RuleScriptQuery queryParams ) {
        IPage<RuleScriptVO> result = ruleScriptService.getRuleScriptPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增规则引擎脚本")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('scene:rule-script:add')")
    public Result<Void> saveRuleScript(@RequestBody @Valid RuleScriptForm formData ) {
        boolean result = ruleScriptService.saveRuleScript(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取规则引擎脚本表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('scene:rule-script:edit')")
    public Result<RuleScriptForm> getRuleScriptForm(
        @Parameter(description = "规则引擎脚本ID") @PathVariable Long id
    ) {
        RuleScriptForm formData = ruleScriptService.getRuleScriptFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改规则引擎脚本")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('scene:rule-script:edit')")
    public Result<Void> updateRuleScript(
            @Parameter(description = "规则引擎脚本ID") @PathVariable Long id,
            @RequestBody @Validated RuleScriptForm formData
    ) {
        boolean result = ruleScriptService.updateRuleScript(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除规则引擎脚本")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('scene:rule-script:delete')")
    public Result<Void> deleteRuleScripts(
        @Parameter(description = "规则引擎脚本ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = ruleScriptService.deleteRuleScripts(ids);
        return Result.judge(result);
    }
}
