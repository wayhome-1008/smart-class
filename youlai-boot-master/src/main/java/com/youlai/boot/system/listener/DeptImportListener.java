package com.youlai.boot.system.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.youlai.boot.common.constant.SystemConstants;
import com.youlai.boot.common.result.ExcelResult;
import com.youlai.boot.system.model.dto.DeptImportDTO;
import com.youlai.boot.system.model.entity.Dept;
import com.youlai.boot.system.service.DeptService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *@Author: way
 *@CreateTime: 2025-09-09  10:59
 *@Description: TODO
 */
@Slf4j
public class DeptImportListener extends AnalysisEventListener<DeptImportDTO> {
    /**
     * Excel 导入结果
     */
    @Getter
    private final ExcelResult excelResult;

    private final DeptService deptService;

    /**
     * 当前行
     */
    private Integer currentRow = 1;

    /**
     * 构造方法
     */
    public DeptImportListener() {
        this.deptService = SpringUtil.getBean(DeptService.class);
        this.excelResult = new ExcelResult();
    }

    @Override
    public void invoke(DeptImportDTO deptImportDTO, AnalysisContext context) {
        log.info("解析到一条部门数据:{}", JSONUtil.toJsonStr(deptImportDTO));
        boolean validation = true;
        String errorMsg = "第" + currentRow + "行数据校验失败：";

        // 部门的名称校验
        String deptName = deptImportDTO.getName();
        if (StrUtil.isBlank(deptName)) {
            errorMsg += "部门名称为空；";
            validation = false;
        } else {
            long count = deptService.count(new LambdaQueryWrapper<Dept>().eq(Dept::getName, deptName));
            if (count > 0) {
                errorMsg += "部门名称已存在；";
                validation = false;
            }
        }

        // 部门编码校验（如果提供了编码）
        String deptCode = deptImportDTO.getCode();
        if (StrUtil.isNotBlank(deptCode)) {
            long count = deptService.count(new LambdaQueryWrapper<Dept>().eq(Dept::getCode, deptCode));
            if (count > 0) {
                errorMsg += "部门编码已存在；";
                validation = false;
            }
        }

        if (validation) {
            // 校验通过，持久化至数据库
            Dept entity = new Dept();
            entity.setName(deptName);
            entity.setCode(deptCode);
            entity.setStatus(1);
            entity.setIsDeleted(0);
            entity.setSort(1); // 默认排序
            // 设置父级部门
            String parentCode = deptImportDTO.getParentCode();
            Long parentId = SystemConstants.ROOT_NODE_ID; // 默认根部门

            if (StrUtil.isNotBlank(parentCode)) {
                Dept parentDept = deptService.getOne(new LambdaQueryWrapper<Dept>().eq(Dept::getCode, parentCode));
                if (parentDept != null) {
                    parentId = parentDept.getId();
                } else {
                    errorMsg += "父级部门编码不存在；";
                    excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                    excelResult.getMessageList().add(errorMsg);
                    currentRow++;
                    return;
                }
            }
            entity.setParentId(parentId);

            // 参照 DeptService 的 saveDept 方法正确处理 treePath
            try {
                // 生成部门路径(tree_path)
                String treePath = generateDeptTreePath(parentId);
                entity.setTreePath(treePath);

                // 设置创建人（使用默认值，实际项目中应获取当前用户ID）
                entity.setCreateBy(1L);
                entity.setUpdateBy(1L);

                boolean saveResult = deptService.save(entity);
                if (saveResult) {
                    excelResult.setValidCount(excelResult.getValidCount() + 1);
                } else {
                    excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                    errorMsg += "第" + currentRow + "行数据保存失败；";
                    excelResult.getMessageList().add(errorMsg);
                }
            } catch (Exception e) {
                excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
                errorMsg += "第" + currentRow + "行数据保存异常：" + e.getMessage();
                excelResult.getMessageList().add(errorMsg);
                log.error("部门导入保存异常", e);
            }
        } else {
            excelResult.setInvalidCount(excelResult.getInvalidCount() + 1);
            excelResult.getMessageList().add(errorMsg);
        }
        currentRow++;
    }

    /**
     * 部门路径生成（参照 DeptServiceImpl 中的 generateDeptTreePath 方法）
     *
     * @param parentId 父ID
     * @return 父节点路径以英文逗号(, )分割，eg: ,1,2,
     */
    private String generateDeptTreePath(Long parentId) {
        String treePath = "";
        if (SystemConstants.ROOT_NODE_ID.equals(parentId)) {
            treePath = ","; // 根节点路径
        } else {
            Dept parent = deptService.getById(parentId);
            if (parent != null) {
                treePath = parent.getTreePath() + parent.getId() + ",";
            } else {
                treePath = ","; // 如果父部门不存在，则使用根路径
            }
        }
        return treePath;
    }


    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成！");
    }
}
