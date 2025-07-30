package com.youlai.boot.scene.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.mapper.ActionMapper;
import com.youlai.boot.scene.service.ActionService;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.form.ActionForm;
import com.youlai.boot.scene.model.query.ActionQuery;
import com.youlai.boot.scene.model.vo.ActionVO;
import com.youlai.boot.scene.converter.ActionConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 执行器服务实现类
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Service
@RequiredArgsConstructor
public class ActionServiceImpl extends ServiceImpl<ActionMapper, Action> implements ActionService {

    private final ActionConverter actionConverter;

    /**
    * 获取执行器分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<ActionVO>} 执行器分页列表
    */
    @Override
    public IPage<ActionVO> getActionPage(ActionQuery queryParams) {
        Page<ActionVO> pageVO = this.baseMapper.getActionPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取执行器表单数据
     *
     * @param id 执行器ID
     * @return 执行器表单数据
     */
    @Override
    public ActionForm getActionFormData(Long id) {
        Action entity = this.getById(id);
        return actionConverter.toForm(entity);
    }
    
    /**
     * 新增执行器
     *
     * @param formData 执行器表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveAction(ActionForm formData) {
        Action entity = actionConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新执行器
     *
     * @param id   执行器ID
     * @param formData 执行器表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateAction(Long id,ActionForm formData) {
        Action entity = actionConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除执行器
     *
     * @param ids 执行器ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteActions(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的执行器数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
