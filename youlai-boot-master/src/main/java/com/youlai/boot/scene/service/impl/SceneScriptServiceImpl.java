package com.youlai.boot.scene.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.mapper.SceneScriptMapper;
import com.youlai.boot.scene.service.SceneScriptService;
import com.youlai.boot.scene.model.entity.SceneScript;
import com.youlai.boot.scene.model.form.SceneScriptForm;
import com.youlai.boot.scene.model.query.SceneScriptQuery;
import com.youlai.boot.scene.model.vo.SceneScriptVO;
import com.youlai.boot.scene.converter.SceneScriptConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 场景脚本服务实现类
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Service
@RequiredArgsConstructor
public class SceneScriptServiceImpl extends ServiceImpl<SceneScriptMapper, SceneScript> implements SceneScriptService {

    private final SceneScriptConverter sceneScriptConverter;

    /**
    * 获取场景脚本分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<SceneScriptVO>} 场景脚本分页列表
    */
    @Override
    public IPage<SceneScriptVO> getSceneScriptPage(SceneScriptQuery queryParams) {
        Page<SceneScriptVO> pageVO = this.baseMapper.getSceneScriptPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }

    /**
     * 获取场景脚本表单数据
     *
     * @param id 场景脚本ID
     * @return 场景脚本表单数据
     */
    @Override
    public SceneScriptForm getSceneScriptFormData(Long id) {
        SceneScript entity = this.getById(id);
        return sceneScriptConverter.toForm(entity);
    }

    /**
     * 新增场景脚本
     *
     * @param formData 场景脚本表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveSceneScript(SceneScriptForm formData) {
        SceneScript entity = sceneScriptConverter.toEntity(formData);
        return this.save(entity);
    }

    /**
     * 更新场景脚本
     *
     * @param id   场景脚本ID
     * @param formData 场景脚本表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateSceneScript(Long id,SceneScriptForm formData) {
        SceneScript entity = sceneScriptConverter.toEntity(formData);
        return this.updateById(entity);
    }

    /**
     * 删除场景脚本
     *
     * @param ids 场景脚本ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteSceneScripts(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的场景脚本数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
