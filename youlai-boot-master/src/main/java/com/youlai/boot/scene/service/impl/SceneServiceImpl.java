package com.youlai.boot.scene.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.scene.converter.SceneConverter;
import com.youlai.boot.scene.liteFlow.SceneFlowBuilder;
import com.youlai.boot.scene.mapper.ActionMapper;
import com.youlai.boot.scene.mapper.SceneMapper;
import com.youlai.boot.scene.mapper.TriggerMapper;
import com.youlai.boot.scene.model.entity.Action;
import com.youlai.boot.scene.model.entity.Scene;
import com.youlai.boot.scene.model.entity.Trigger;
import com.youlai.boot.scene.model.form.SceneForm;
import com.youlai.boot.scene.model.query.SceneQuery;
import com.youlai.boot.scene.model.vo.SceneVO;
import com.youlai.boot.scene.service.SceneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 场景交互服务实现类
 *
 * @author way
 * @since 2025-07-29 11:51
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SceneServiceImpl extends ServiceImpl<SceneMapper, Scene> implements SceneService {

    private final SceneConverter sceneConverter;
    private final TriggerMapper triggerMapper;
    private final ActionMapper actionMapper;
    private final SceneFlowBuilder flowBuilder;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建 ApplicationRunner Bean 来初始化场景
     */
    @Bean
    @Order(100)
    public ApplicationRunner initScenesToLiteFlowRunner() {
        return args -> {
            log.info("等待LiteFlow组件初始化完成...");
            // 等待LiteFlow组件扫描完成
            Thread.sleep(5000); // 增加等待时间到5秒
            initScenesToLiteFlow();
        };
    }

    /**
     * 应用上下文刷新完成后初始化场景
     */
//    @EventListener
//    public void handleContextRefresh(ContextRefreshedEvent event) {
//        // 确保是根应用上下文
//        if (event.getApplicationContext().getParent() == null) {
//            // 延迟执行，确保LiteFlow完全初始化
//            new Thread(() -> {
//                try {
//                    // 循环检查组件是否已注册
//                    int retryCount = 0;
//                    while (retryCount < 10) {
//                        if (checkRequiredComponents()) {
//                            log.info("LiteFlow组件已就绪，开始初始化场景");
//                            initScenesToLiteFlow();
//                            break;
//                        } else {
//                            log.info("LiteFlow组件未就绪，等待中... ({}/10)", retryCount + 1);
//                            Thread.sleep(1000);
//                            retryCount++;
//                        }
//                    }
//
//                    if (retryCount >= 10) {
//                        log.error("LiteFlow组件初始化超时，跳过场景初始化");
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    log.error("场景初始化被中断", e);
//                } catch (Exception e) {
//                    log.error("场景初始化失败", e);
//                }
//            }).start();
//        }
//    }

//    /**
//     * 检查必需的LiteFlow组件是否已注册
//     */
//    private boolean checkRequiredComponents() {
//        try {
//            boolean deviceTriggerExists = com.yomahub.liteflow.flow.FlowBus.containNode("deviceTrigger");
//            boolean silenceCheckExists = com.yomahub.liteflow.flow.FlowBus.containNode("silenceCheck");
//            boolean delayExecuteExists = com.yomahub.liteflow.flow.FlowBus.containNode("delayExecute");
//            boolean deviceExecuteExists = com.yomahub.liteflow.flow.FlowBus.containNode("deviceExecute");
//
//            log.info("组件存在性检查: deviceTrigger={}, silenceCheck={}, delayExecute={}, deviceExecute={}",
//                    deviceTriggerExists, silenceCheckExists, delayExecuteExists, deviceExecuteExists);
//
//            return deviceTriggerExists && silenceCheckExists && delayExecuteExists && deviceExecuteExists;
//        } catch (Exception e) {
//            log.warn("检查组件存在性时出错: {}", e.getMessage());
//            return false;
//        }
//    }

    /**
     * 初始化所有场景的设备索引到Redis
     */
    public void initDeviceSceneIndex(List<Scene> scenes) {
        log.info("开始初始化设备场景索引到Redis...");

        try {
            // 清理旧的设备场景索引
            cleanDeviceSceneIndex();

            int sceneCount = 0;
            int indexCount = 0;


            // 构建设备场景索引
            for (Scene scene : scenes) {
                buildDeviceSceneIndexForScene(scene);

                sceneCount++;
                indexCount += (scene.getTriggers().size() + scene.getActions().size());
            }

            log.info("设备场景索引初始化完成，共处理 {} 个场景，建立 {} 个索引关系", sceneCount, indexCount);
        } catch (Exception e) {
            log.error("初始化设备场景索引到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理旧的设备场景索引
     */
    private void cleanDeviceSceneIndex() {
        // 获取所有设备场景索引的key
        Set<String> keys = redisTemplate.keys("device:*:scenes");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("已清理 {} 个旧的设备场景索引", keys.size());
        }
    }

    /**
     * 为单个场景构建设备场景索引
     */
    private void buildDeviceSceneIndexForScene(Scene scene) {
        // 提取场景中的所有设备ID
        Set<String> deviceCodes = extractDeviceIdsFromScene(scene);

        // 为每个设备建立索引
        String sceneIdStr = String.valueOf(scene.getId());
        for (String deviceCode : deviceCodes) {
            String key = "device:" + deviceCode + ":scenes";
            // 将场景ID添加到设备对应的场景集合中
            redisTemplate.opsForSet().add(key, sceneIdStr);
        }

        // 同时将场景详情缓存到Redis中
        String sceneKey = "scene:" + scene.getId();
        redisTemplate.opsForValue().set(sceneKey, scene);

        log.debug("为场景 {}[ID:{}] 建立了 {} 个设备索引", scene.getSceneName(), scene.getId(), deviceCodes.size());
    }

    /**
     * 提取场景中所有设备ID的方法
     */
    private Set<String> extractDeviceIdsFromScene(Scene scene) {
        Set<String> deviceCode = new HashSet<>();

        // 从触发器中提取设备ID
        if (scene.getTriggers() != null) {
            for (Trigger trigger : scene.getTriggers()) {
                if (trigger.getDeviceCodes() != null && !trigger.getDeviceCodes().isEmpty()) {
                    String[] codes = trigger.getDeviceCodes().split(",");
                    deviceCode.addAll(Arrays.asList(codes));
                }
            }
        }

        // 从动作中提取设备ID
        if (scene.getActions() != null) {
            for (Action action : scene.getActions()) {
                if (action.getDeviceCodes() != null && !action.getDeviceCodes().isEmpty()) {
                    String[] ids = action.getDeviceCodes().split(",");
                    deviceCode.addAll(Arrays.asList(ids));
                }
            }
        }

        return deviceCode;
    }

    /**
     * 初始化所有场景到LiteFlow
     */
    public void initScenesToLiteFlow() {
        log.info("开始初始化场景到LiteFlow...");
        try {
            // 查询所有启用的场景
            LambdaQueryWrapper<Scene> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Scene::getEnable, 1); // 假设1表示启用
            List<Scene> scenes = this.list(queryWrapper);
//            log.info("找到 {} 个启用的场景", scenes.size());
            // 为每个场景注册流程
            for (Scene scene : scenes) {
                try {
                    // 加载关联的触发器
                    List<Trigger> triggers = triggerMapper.selectList(
                            new LambdaQueryWrapper<Trigger>().eq(Trigger::getSceneId, scene.getId())
                    );
                    scene.setTriggers(triggers);

                    // 加载关联的动作
                    List<Action> actions = actionMapper.selectList(
                            new LambdaQueryWrapper<Action>().eq(Action::getSceneId, scene.getId())
                    );
                    scene.setActions(actions);
                    // 注册到LiteFlow
                    flowBuilder.registerFlow(scene);
//                    log.info("场景 {}[ID:{}] 已注册到LiteFlow", scene.getSceneName(), scene.getId());

                } catch (Exception e) {
                    log.error("注册场景 {}[ID:{}] 到LiteFlow失败: {}", scene.getSceneName(), scene.getId(), e.getMessage(), e);
                }
            }
            log.info("场景初始化完成，共注册 {} 个场景", scenes.size());

            // 初始化设备场景索引
            initDeviceSceneIndex(scenes);
        } catch (Exception e) {
            log.error("初始化场景到LiteFlow失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据设备ID获取相关场景列表
     * @param deviceCode 设备Code
     * @return 场景列表
     */
    @Override
    public List<Scene> getScenesByDeviceCode(String deviceCode) {
        return getScenesByDeviceCode(deviceCode, true);
    }

    /**
     * 根据设备ID获取相关场景列表
     * @param deviceCode 设备Code
     * @param onlyEnabled 是否只返回启用的场景
     * @return 场景列表
     */
    public List<Scene> getScenesByDeviceCode(String deviceCode, boolean onlyEnabled) {
        // 从Redis中获取设备关联的场景ID列表
        String key = "device:" + deviceCode + ":scenes";
        Set<Object> sceneIdsObj = redisTemplate.opsForSet().members(key);

        if (sceneIdsObj != null && !sceneIdsObj.isEmpty()) {
            // 类型转换：将Set<Object>转换为Set<String>
            Set<String> sceneIds = new HashSet<>();
            for (Object obj : sceneIdsObj) {
                sceneIds.add(obj.toString());
            }

            List<Scene> result = new ArrayList<>();

            // 查询场景详情
            for (String sceneIdStr : sceneIds) {
                Long sceneId = Long.valueOf(sceneIdStr);

                // 从Redis中获取场景详情
                String sceneKey = "scene:" + sceneId;
                Scene scene = (Scene) redisTemplate.opsForValue().get(sceneKey);

                // 如果Redis中没有缓存，则从数据库查询并缓存
                if (scene == null) {
                    scene = this.getById(sceneId);
                    if (scene != null) {
                        // 加载关联的触发器
                        List<Trigger> triggers = triggerMapper.selectList(
                                new LambdaQueryWrapper<Trigger>().eq(Trigger::getSceneId, scene.getId())
                        );
                        scene.setTriggers(triggers);

                        // 加载关联的动作
                        List<Action> actions = actionMapper.selectList(
                                new LambdaQueryWrapper<Action>().eq(Action::getSceneId, scene.getId())
                        );
                        scene.setActions(actions);

                        // 缓存到Redis
                        redisTemplate.opsForValue().set(sceneKey, scene);
                    }
                }

                // 过滤启用的场景（如果需要）
                if (scene != null && (!onlyEnabled || scene.getEnable() == 1)) {
                    result.add(scene);
                }
            }

            return result;
        }

        return Collections.emptyList();
    }

    /**
     * 获取场景交互分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<SceneVO>} 场景交互分页列表
     */
    @Override
    public IPage<SceneVO> getScenePage(SceneQuery queryParams) {
        return this.baseMapper.getScenePage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
    }

    /**
     * 获取场景交互表单数据
     *
     * @param id 场景交互ID
     * @return 场景交互表单数据
     */
    @Override
    public SceneForm getSceneFormData(Long id) {
        Scene entity = this.getById(id);
        return sceneConverter.toForm(entity);
    }

    /**
     * 新增场景交互
     *
     * @param formData 场景交互表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveScene(SceneForm formData) {
        if (formData.getTriggers().isEmpty() || formData.getActions().isEmpty()) {
            log.error("场景中至少包含一个触发器或者执行动作，否则规则启动报错！");
            return false;
        }
        Scene scene = sceneConverter.toEntity(formData);
        boolean result = this.save(scene);
        // 保存关联的触发器
        if (scene.getTriggers() != null) {
            for (Trigger trigger : scene.getTriggers()) {
                trigger.setSceneId(scene.getId());
                triggerMapper.insert(trigger);
            }
        }

        // 保存关联的动作
        if (scene.getActions() != null) {
            for (Action action : scene.getActions()) {
                action.setSceneId(scene.getId());
                actionMapper.insert(action);
            }
        }
        // 注册流程
        flowBuilder.registerFlow(scene);
        // 同步到Redis
        syncSceneToRedis(scene);
        return result;

    }

    /**
     * 更新场景交互
     *
     * @param id   场景交互ID
     * @param formData 场景交互表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateScene(Long id, SceneForm formData) {
        Scene oldScene = this.getById(id);
        Scene entity = sceneConverter.toEntity(formData);
        boolean result = this.updateById(entity);

        if (result) {
            // 删除旧的索引
            if (oldScene != null) {
                removeDeviceSceneIndex(oldScene);
            }

            // 更新关联的触发器和动作
            // 先删除旧的
            triggerMapper.delete(new LambdaQueryWrapper<Trigger>().eq(Trigger::getSceneId, id));
            actionMapper.delete(new LambdaQueryWrapper<Action>().eq(Action::getSceneId, id));

            // 再插入新的
            if (entity.getTriggers() != null) {
                for (Trigger trigger : entity.getTriggers()) {
                    trigger.setSceneId(entity.getId());
                    triggerMapper.insert(trigger);
                }
            }

            if (entity.getActions() != null) {
                for (Action action : entity.getActions()) {
                    action.setSceneId(entity.getId());
                    actionMapper.insert(action);
                }
            }
            // 同步到Redis
            syncSceneToRedis(entity);
        }

        return result;
    }

    /**
     * 删除场景交互
     *
     * @param ids 场景交互ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteScenes(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的场景交互数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).toList();
        for (Long sceneId : idList) {
            Scene scene = this.getById(sceneId);
            if (scene != null) {
                // 删除索引
                removeDeviceSceneIndex(scene);
                // 从Redis中删除场景缓存
                String sceneKey = "scene:" + sceneId;
                redisTemplate.delete(sceneKey);
            }
        }

        return this.removeByIds(idList);
    }

    /**
     * 同步场景到Redis
     */
    private void syncSceneToRedis(Scene scene) {
        try {
            // 加载完整的触发器和动作信息
            List<Trigger> triggers = triggerMapper.selectList(
                    new LambdaQueryWrapper<Trigger>().eq(Trigger::getSceneId, scene.getId())
            );
            scene.setTriggers(triggers);

            List<Action> actions = actionMapper.selectList(
                    new LambdaQueryWrapper<Action>().eq(Action::getSceneId, scene.getId())
            );
            scene.setActions(actions);

            // 更新设备场景索引
            updateDeviceSceneIndex(scene);

            // 缓存场景详情到Redis
            String sceneKey = "scene:" + scene.getId();
            redisTemplate.opsForValue().set(sceneKey, scene);

            log.debug("场景 {}[ID:{}] 已同步到Redis", scene.getSceneName(), scene.getId());
        } catch (Exception e) {
            log.error("同步场景 {}[ID:{}] 到Redis失败: {}", scene.getSceneName(), scene.getId(), e.getMessage(), e);
        }
    }

    /**
     * 更新设备场景索引
     */
    private void updateDeviceSceneIndex(Scene scene) {
        // 先删除旧的索引
        removeDeviceSceneIndex(scene);

        // 提取场景中的所有设备ID
        Set<String> deviceIds = extractDeviceIdsFromScene(scene);

        // 为每个设备建立索引
        String sceneIdStr = String.valueOf(scene.getId());
        for (String deviceId : deviceIds) {
            String key = "device:" + deviceId + ":scenes";
            // 将场景ID添加到设备对应的场景集合中
            redisTemplate.opsForSet().add(key, sceneIdStr);
        }
    }

    /**
     * 删除设备场景索引
     */
    private void removeDeviceSceneIndex(Scene scene) {
        // 提取场景中的所有设备ID
        Set<String> deviceIds = extractDeviceIdsFromScene(scene);

        // 从每个设备的场景集合中移除该场景ID
        String sceneIdStr = String.valueOf(scene.getId());
        for (String deviceId : deviceIds) {
            String key = "device:" + deviceId + ":scenes";
            redisTemplate.opsForSet().remove(key, sceneIdStr);
        }
    }
}
