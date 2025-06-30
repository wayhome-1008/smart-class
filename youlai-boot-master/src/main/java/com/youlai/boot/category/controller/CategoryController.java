package com.youlai.boot.category.controller;

import com.youlai.boot.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.youlai.boot.category.model.form.CategoryForm;
import com.youlai.boot.category.model.query.CategoryQuery;
import com.youlai.boot.category.model.vo.CategoryVO;
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
 * 分类管理前端控制层
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Tag(name = "分类管理接口")
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController  {

    private final CategoryService categoryService;

    @Operation(summary = "分类管理分页列表")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasPerm('category:category:query')")
    public PageResult<CategoryVO> getCategoryPage(CategoryQuery queryParams ) {
        IPage<CategoryVO> result = categoryService.getCategoryPage(queryParams);
        return PageResult.success(result);
    }

    @Operation(summary = "新增分类管理")
    @PostMapping
    @PreAuthorize("@ss.hasPerm('category:category:add')")
    public Result<Void> saveCategory(@RequestBody @Valid CategoryForm formData ) {
        boolean result = categoryService.saveCategory(formData);
        return Result.judge(result);
    }

    @Operation(summary = "获取分类管理表单数据")
    @GetMapping("/{id}/form")
    @PreAuthorize("@ss.hasPerm('category:category:edit')")
    public Result<CategoryForm> getCategoryForm(
        @Parameter(description = "分类管理ID") @PathVariable Long id
    ) {
        CategoryForm formData = categoryService.getCategoryFormData(id);
        return Result.success(formData);
    }

    @Operation(summary = "修改分类管理")
    @PutMapping(value = "/{id}")
    @PreAuthorize("@ss.hasPerm('category:category:edit')")
    public Result<Void> updateCategory(
            @Parameter(description = "分类管理ID") @PathVariable Long id,
            @RequestBody @Validated CategoryForm formData
    ) {
        boolean result = categoryService.updateCategory(id, formData);
        return Result.judge(result);
    }

    @Operation(summary = "删除分类管理")
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('category:category:delete')")
    public Result<Void> deleteCategorys(
        @Parameter(description = "分类管理ID，多个以英文逗号(,)分割") @PathVariable String ids
    ) {
        boolean result = categoryService.deleteCategorys(ids);
        return Result.judge(result);
    }
}
