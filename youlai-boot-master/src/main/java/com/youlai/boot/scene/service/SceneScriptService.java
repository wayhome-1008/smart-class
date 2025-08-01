package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.SceneScript;
import com.youlai.boot.scene.model.form.SceneScriptForm;
import com.youlai.boot.scene.model.query.SceneScriptQuery;
import com.youlai.boot.scene.model.vo.SceneScriptVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 场景脚本服务类
 *
 * @author way
 * @since 2025-07-29 11:58
 */
public interface SceneScriptService extends IService<SceneScript> {

    /**
     *场景脚本分页列表
     *
     * @return {@link IPage<SceneScriptVO>} 场景脚本分页列表
     */
    IPage<SceneScriptVO> getSceneScriptPage(SceneScriptQuery queryParams);

    /**
     * 获取场景脚本表单数据
     *
     * @param id 场景脚本ID
     * @return 场景脚本表单数据
     */
     SceneScriptForm getSceneScriptFormData(Long id);

    /**
     * 新增场景脚本
     *
     * @param formData 场景脚本表单对象
     * @return 是否新增成功
     */
    boolean saveSceneScript(SceneScriptForm formData);

    /**
     * 修改场景脚本
     *
     * @param id   场景脚本ID
     * @param formData 场景脚本表单对象
     * @return 是否修改成功
     */
    boolean updateSceneScript(Long id, SceneScriptForm formData);

    /**
     * 删除场景脚本
     *
     * @param ids 场景脚本ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteSceneScripts(String ids);

}
