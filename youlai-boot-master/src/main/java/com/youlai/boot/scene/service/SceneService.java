package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.form.SceneForm;
import com.youlai.boot.scene.model.query.SceneQuery;
import com.youlai.boot.scene.model.vo.SceneVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 场景交互服务类
 *
 * @author way
 * @since 2025-07-29 11:51
 */
public interface SceneService extends IService<Scene> {

    /**
     *场景交互分页列表
     *
     * @return {@link IPage<SceneVO>} 场景交互分页列表
     */
    IPage<SceneVO> getScenePage(SceneQuery queryParams);

    /**
     * 获取场景交互表单数据
     *
     * @param id 场景交互ID
     * @return 场景交互表单数据
     */
     SceneForm getSceneFormData(Long id);

    /**
     * 新增场景交互
     *
     * @param formData 场景交互表单对象
     * @return 是否新增成功
     */
    boolean saveScene(SceneForm formData);

    /**
     * 修改场景交互
     *
     * @param id   场景交互ID
     * @param formData 场景交互表单对象
     * @return 是否修改成功
     */
    boolean updateScene(Long id, SceneForm formData);

    /**
     * 删除场景交互
     *
     * @param ids 场景交互ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteScenes(String ids);

    List<Scene> getScenesByDeviceCode(String deviceCode);
}
