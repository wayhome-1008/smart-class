package com.youlai.boot.system.controller;

import com.youlai.boot.common.annotation.Log;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.system.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.system.model.form.AlertRuleForm;
import com.youlai.boot.system.model.query.AlertRuleQuery;
import com.youlai.boot.system.model.vo.AlertRuleVO;
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
 * 报警配置前端控制层
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Tag(name = "报警配置接口")
@RestController
@RequestMapping("/api/v1/alert-rule")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @Operation(summary = "报警配置分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('system:alertRule:query')")
    public PageResult<AlertRuleVO> getAlertRulePage(AlertRuleQuery queryParams) {
        IPage<AlertRuleVO> result = alertRuleService.getAlertRulePage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增报警配置")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('system:alertRule:add')")
    @Log(value = "新增报警配置",module = LogModuleEnum.ALERT_RULE)
    public Result<Void> saveAlertRule(@RequestBody @Valid AlertRuleForm formData) {
        boolean result = alertRuleService.saveAlertRule(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取报警配置表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('system:alertRule:edit')")
    public Result<AlertRuleForm> getAlertRuleForm(
            @Parameter(description = "报警配置ID") @PathVariable Long id
    ) {
        AlertRuleForm formData = alertRuleService.getAlertRuleFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改报警配置")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('system:alertRule:edit')")
    @Log(value = "修改报警配置",module = LogModuleEnum.ALERT_RULE)
    public Result<Void> updateAlertRule(
            @Parameter(description = "报警配置ID") @PathVariable Long id,
            @RequestBody @Validated AlertRuleForm formData
    ) {
        boolean result = alertRuleService.updateAlertRule(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除报警配置")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('system:alertRule:delete')")
    @Log(value = "删除报警配置",module = LogModuleEnum.ALERT_RULE)
    public Result<Void> deleteAlertRules(
            @Parameter(description = "报警配置ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = alertRuleService.deleteAlertRules(ids);
        return Result.judge(result);
    }
}
