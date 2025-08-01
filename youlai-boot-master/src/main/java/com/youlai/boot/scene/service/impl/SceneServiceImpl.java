package com.youlai.boot.scene.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
     * 初始化所有场景到LiteFlow
     */
    public void initScenesToLiteFlow() {
        log.info("开始初始化场景到LiteFlow...");
        try {
            // 查询所有启用的场景
            LambdaQueryWrapper<Scene> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Scene::getEnable, 1); // 假设1表示启用
            List<Scene> scenes = this.list(queryWrapper);
            log.info("找到 {} 个启用的场景", scenes.size());

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
                    log.info("场景 {}[ID:{}] 已注册到LiteFlow", scene.getSceneName(), scene.getId());

                } catch (Exception e) {
                    log.error("注册场景 {}[ID:{}] 到LiteFlow失败: {}", scene.getSceneName(), scene.getId(), e.getMessage(), e);
                }
            }

            log.info("场景初始化完成，共注册 {} 个场景", scenes.size());

        } catch (Exception e) {
            log.error("初始化场景到LiteFlow失败: {}", e.getMessage(), e);
        }
    }
    /**
     * 应用上下文刷新完成后初始化场景
     */
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        // 确保是根应用上下文
        if (event.getApplicationContext().getParent() == null) {
            // 延迟执行，确保LiteFlow完全初始化
            new Thread(() -> {
                try {
                    // 循环检查组件是否已注册
                    int retryCount = 0;
                    while (retryCount < 10) {
                        if (checkRequiredComponents()) {
                            log.info("LiteFlow组件已就绪，开始初始化场景");
                            initScenesToLiteFlow();
                            break;
                        } else {
                            log.info("LiteFlow组件未就绪，等待中... ({}/10)", retryCount + 1);
                            Thread.sleep(1000);
                            retryCount++;
                        }
                    }

                    if (retryCount >= 10) {
                        log.error("LiteFlow组件初始化超时，跳过场景初始化");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("场景初始化被中断", e);
                } catch (Exception e) {
                    log.error("场景初始化失败", e);
                }
            }).start();
        }
    }

    /**
     * 检查必需的LiteFlow组件是否已注册
     */
    private boolean checkRequiredComponents() {
        try {
            boolean deviceTriggerExists = com.yomahub.liteflow.flow.FlowBus.containNode("deviceTrigger");
            boolean silenceCheckExists = com.yomahub.liteflow.flow.FlowBus.containNode("silenceCheck");
            boolean delayExecuteExists = com.yomahub.liteflow.flow.FlowBus.containNode("delayExecute");
            boolean deviceExecuteExists = com.yomahub.liteflow.flow.FlowBus.containNode("deviceExecute");

            log.info("组件存在性检查: deviceTrigger={}, silenceCheck={}, delayExecute={}, deviceExecute={}",
                    deviceTriggerExists, silenceCheckExists, delayExecuteExists, deviceExecuteExists);

            return deviceTriggerExists && silenceCheckExists && delayExecuteExists && deviceExecuteExists;
        } catch (Exception e) {
            log.warn("检查组件存在性时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取场景交互分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<SceneVO>} 场景交互分页列表
     */
    @Override
    public IPage<SceneVO> getScenePage(SceneQuery queryParams) {
        Page<SceneVO> pageVO = this.baseMapper.getScenePage(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), queryParams);
        return pageVO;
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
        this.save(scene);
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
        return true;

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
        Scene entity = sceneConverter.toEntity(formData);
        return this.updateById(entity);
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
        return this.removeByIds(idList);
    }

    /**
     * 构建EL数据
     *
     * @param scene        场景
     * @param ruleScripts  规则脚本集合
     * @param sceneScripts 场景脚本集合
     * @param sceneDevices 场景设备集合
     * @return
     */
//    private String buildElData(Scene scene, List<RuleScript> ruleScripts, List<SceneScript> sceneScripts, List<SceneDevice> sceneDevices) {
//        // 排除定时后的触发器，等于0不生成规则数据，等于1移除AND和OR
//        Long triggerNodeCount = scene.getTriggers().stream().filter(x -> x.getSource() != 2).count();
//        Long triggerTimingCount = scene.getTriggers().stream().filter(x -> x.getSource() == 2).count();
//
//        // 拼接规则数据，格式如：IF(AND(T1,T2,T3),THEN(A1,A2,A3))
//        StringBuilder triggerBuilder = new StringBuilder();
//        StringBuilder actionBuilder = new StringBuilder();
//        if (0 == triggerNodeCount && triggerTimingCount != 0) {
//            switch (scene.getExecuteMode()) {
//                case 1:
//                    actionBuilder.append("THEN(");
//                    break;
//                case 2:
//                    actionBuilder.append("WHEN(");
//                    break;
//                default:
//                    break;
//            }
//        } else {
//            switch (scene.getCond()) {
//                case 1:
//                    triggerBuilder.append("IF(OR(");
//                    break;
//                case 2:
//                    triggerBuilder.append("IF(AND(");
//                    break;
//                case 3:
//                    triggerBuilder.append("IF(NOT(");
//                    break;
//                default:
//                    break;
//            }
//            switch (scene.getExecuteMode()) {
//                case 1:
//                    actionBuilder.append(",THEN(");
//                    break;
//                case 2:
//                    actionBuilder.append(",WHEN(");
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        for (int i = 0; i < scene.getTriggers().size(); i++) {
//            // 保存触发器和执行动作
//            String scriptId = buildTriggerAction(scene.getTriggers().get(i), 2, scene, ruleScripts, sceneScripts, sceneDevices);
//            // 构建触发器EL，排除定时触发器
//            if (scene.getTriggers().get(i).getSource() != 2) {
//                triggerBuilder.append(scriptId + ",");
//            }
//        }
//        if (triggerNodeCount > 0) {
//            triggerBuilder.deleteCharAt(triggerBuilder.lastIndexOf(","));
//            triggerBuilder.append(")");
//        }
//
//        for (int i = 0; i < scene.getActions().size(); i++) {
//            // 保存触发器和执行动作
//            String scriptId = buildTriggerAction(scene.getActions().get(i), 3, scene, ruleScripts, sceneScripts, sceneDevices);
//            // 构建执行动作EL
//            actionBuilder.append(scriptId + ",");
//        }
//        if (scene.getActions().size() > 0) {
//            actionBuilder.deleteCharAt(actionBuilder.lastIndexOf(","));
//        }
//        String elData;
//        if (StringUtils.isEmpty(triggerBuilder)) {
//            actionBuilder.append(")");
//            elData = actionBuilder.toString();
//        } else {
//            actionBuilder.append("))");
//            elData = triggerBuilder.append(actionBuilder).toString();
//        }
//        if (triggerNodeCount == 1) {
//            // 移除AND和OR，它们必须包含两个以上组件
//            if (elData.indexOf("AND(") != -1) {
//                elData = elData.replace("AND(", "").replace("),", ",");
//            } else if (elData.indexOf("OR(") != -1) {
//                elData = elData.replace("OR(", "").replace("),", ",");
//            }
//        }
//        return elData;
//    }

    /**
     * 构建场景中的触发器和执行动作
     *
     * @param sceneScript   场景脚本
     * @param scriptPurpose 脚本用途：1=数据流，2=触发器，3=执行动作
     * @param scene         场景
     * @return 返回规则脚本ID
     */
//    private String buildTriggerAction(SceneScript sceneScript, int scriptPurpose, Scene scene, List<RuleScript> ruleScripts, List<SceneScript> sceneScripts, List<SceneDevice> sceneDevices) {
//        // 构建规则脚本
//        RuleScript ruleScript = new RuleScript();
//        // 设置脚本标识,D=数据流，A=执行动作，T=触发器,雪花算法生成唯一数
//        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
//        String scriptId = String.valueOf(snowflake.nextId());
//        if (scriptPurpose == 2) {
//            scriptId = "T" + scriptId;
//            ruleScript.setScriptType("if_script");
//        } else if (scriptPurpose == 3) {
//            scriptId = "A" + scriptId;
//            ruleScript.setScriptType("script");
//        }
//        ruleScript.setScriptId(scriptId);
//        ruleScript.setScriptName(scriptId);
//        ruleScript.setApplicationName("way");
//        ruleScript.setScriptLanguage("groovy");
//        ruleScript.setScriptPurpose(scriptPurpose);
//        ruleScript.setEnable(1);
//        ruleScript.setScriptOrder(0);
//        ruleScript.setScriptEvent(0);
//        ruleScript.setScriptAction(0);
//        ruleScript.setSceneId(scene.getId());
//        // 构建脚本内容
//        ScriptTemplate template = new ScriptTemplate(
//                scene.getId(), scriptId, sceneScript.getType(), sceneScript.getSource(), scene.getSilentPeriod(),
//                scene.getExecuteDelay(), scene.getCond(), sceneScript.getScriptPurpose(), sceneScript.getModeId(), sceneScript.getModeValue(),
//                StringUtils.isEmpty(sceneScript.getOperator()) ? "=" : sceneScript.getOperator(), String.join(",", sceneScript.getDeviceNums()), sceneScript.getCategoryId()
//        );
//        String data = String.format("String json =\"%s\";\n" + "sceneContext.process(json);", StringEscapeUtils.escapeJava(JSONObject.toJSONString(template)));
//        ruleScript.setScriptData(data);
//        // 构建脚本集合,规则脚本不需要存储定时触发器
//        if (sceneScript.getSource() != 2) {
//            ruleScripts.add(ruleScript);
//        }
//
//        if (scriptPurpose == 2) {
//            // 触发器
//            if (sceneScript.getSource() == 1) {
//                // 构建场景设备集合
//                for (int j = 0; j < sceneScript.getDeviceNums().length; j++) {
//                    SceneDevice sceneDevice = new SceneDevice();
//                    sceneDevice.setSceneId(scene.getId());
//                    sceneDevice.setScriptId(scriptId);
//                    sceneDevice.setSource(sceneScript.getSource());
//                    sceneDevice.setSerialNumber(sceneScript.getDeviceNums()[j]);
//                    sceneDevice.setCategoryId(0L);
//                    sceneDevice.setCategoryName("");
//                    sceneDevice.setType(scriptPurpose);
//                    sceneDevices.add(sceneDevice);
//                }
//            } else if (sceneScript.getSource() == 2) {
//                // 创建告警定时任务
//                createSceneTask(scene, sceneScript);
//            } else if (sceneScript.getSource() == 3) {
//                // 构建场景设备集合
//                SceneDevice sceneDevice = new SceneDevice();
//                sceneDevice.setSceneId(scene.getId());
//                sceneDevice.setScriptId(scriptId);
//                sceneDevice.setSource(sceneScript.getSource());
//                sceneDevice.setSerialNumber("");
//                sceneDevice.setCategoryId(sceneScript.getCategoryId());
//                sceneDevice.setCategoryName(sceneScript.getCategoryName());
//                sceneDevice.setType(scriptPurpose);
//                sceneDevices.add(sceneDevice);
//            }
//        } else if (scriptPurpose == 3) {
//            if (sceneScript.getSource() == 1) {
//                // 构建场景设备集合
//                for (int j = 0; j < sceneScript.getDeviceNums().length; j++) {
//                    SceneDevice sceneDevice = new SceneDevice();
//                    sceneDevice.setSceneId(scene.getId());
//                    sceneDevice.setScriptId(scriptId);
//                    sceneDevice.setSource(sceneScript.getSource());
//                    sceneDevice.setSerialNumber(sceneScript.getDeviceNums()[j]);
//                    sceneDevice.setCategoryId(0L);
//                    sceneDevice.setCategoryName("");
//                    sceneDevice.setType(scriptPurpose);
//                    sceneDevices.add(sceneDevice);
//                }
//            } else if (sceneScript.getSource() == 3) {
//                // 构建场景设备集合
//                SceneDevice sceneDevice = new SceneDevice();
//                sceneDevice.setSceneId(scene.getId());
//                sceneDevice.setScriptId(scriptId);
//                sceneDevice.setSource(sceneScript.getSource());
//                sceneDevice.setSerialNumber("");
//                sceneDevice.setCategoryId(sceneScript.getCategoryId());
//                sceneDevice.setCategoryName(sceneScript.getCategoryName());
//                sceneDevice.setType(scriptPurpose);
//                sceneDevices.add(sceneDevice);
//            }
//        }
//
//        // 构建场景脚本集合
//        sceneScript.setSceneId(scene.getId());
//        sceneScript.setScriptId(scriptId);
//        sceneScript.setOperator(StringUtils.isEmpty(sceneScript.getOperator()) ? "=" : sceneScript.getOperator());
//        sceneScripts.add(sceneScript);
//        // 返回脚本ID
//        return scriptId;
//    }

    /**
     * 创建场景定时任务
     *
     * @param scene
     * @param sceneScript
     */
//    private void createSceneTask(Scene scene, SceneScript sceneScript) {
//        // 创建定时任务
//        try {
//            if (!CronUtils.isValid(sceneScript.getCron())) {
//                log.error("新增场景联动定时任务失败，Cron表达式不正确");
//                throw new Exception("新增场景联动定时任务失败，Cron表达式不正确");
//            }
//            DeviceJob deviceJob = new DeviceJob();
//            deviceJob.setJobName("场景联动定时触发");
//            deviceJob.setJobType(3L);
//            deviceJob.setJobGroup("DEFAULT");
//            deviceJob.setConcurrent(1);
////            deviceJob.setMisfirePolicy("2");
//            deviceJob.setStatus(scene.getEnable() == 1 ? 0 : 1);
//            deviceJob.setCron(sceneScript.getCron());
//            deviceJob.setIsAdvance(sceneScript.getIsAdvance());
//            deviceJob.setSceneId(scene.getId());
//            deviceJob.setDeviceName("场景联动定时触发");
//            deviceJobMapper.save(deviceJob);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 动态构建规则链和脚本组件
     * @param ruleScripts
     */
//    private void dynamicBuildRule(List<RuleScript> ruleScripts, Scene scene) {
//        for (RuleScript ruleScript : ruleScripts) {
//            // 规则引擎构建脚本组件
//            if (ruleScript.getScriptPurpose() == 2) {
//                //脚本条件组件
//                LiteFlowNodeBuilder.createScriptBooleanNode().setId(ruleScript.getScriptId())
//                        .setName(ruleScript.getScriptName())
//                        .setScript(ruleScript.getScriptData())
//                        .build();
//            } else if (ruleScript.getScriptPurpose() == 3) {
//                // 普通组件
//                LiteFlowNodeBuilder.createScriptNode().setId(ruleScript.getScriptId())
//                        .setName(ruleScript.getScriptName())
//                        .setScript(ruleScript.getScriptData())
//                        .build();
//            }
//        }
//        // 构建规则链
//        LiteFlowChainELBuilder.createChain().setChainName(scene.getChainName()).setEL(scene.getElData()).build();
//    }
}
