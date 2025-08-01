package com.youlai.boot.scene.service;

import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.SceneDevice;
import com.youlai.boot.scene.model.form.SceneDeviceForm;
import com.youlai.boot.scene.model.query.SceneDeviceQuery;
import com.youlai.boot.scene.model.vo.SceneDeviceVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 场景设备服务类
 *
 * @author way
 * @since 2025-07-29 12:03
 */
public interface SceneDeviceService extends IService<SceneDevice> {

    /**
     *场景设备分页列表
     *
     * @return {@link IPage<SceneDeviceVO>} 场景设备分页列表
     */
    IPage<SceneDeviceVO> getSceneDevicePage(SceneDeviceQuery queryParams);

    /**
     * 获取场景设备表单数据
     *
     * @param id 场景设备ID
     * @return 场景设备表单数据
     */
     SceneDeviceForm getSceneDeviceFormData(Long id);

    /**
     * 新增场景设备
     *
     * @param formData 场景设备表单对象
     * @return 是否新增成功
     */
    boolean saveSceneDevice(SceneDeviceForm formData);

    /**
     * 修改场景设备
     *
     * @param id   场景设备ID
     * @param formData 场景设备表单对象
     * @return 是否修改成功
     */
    boolean updateSceneDevice(Long id, SceneDeviceForm formData);

    /**
     * 删除场景设备
     *
     * @param ids 场景设备ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    boolean deleteSceneDevices(String ids);

    List<Scene> selectTriggerDeviceRelateScenes(SceneDevice sceneDevice);
}
