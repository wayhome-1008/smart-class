//package com.youlai.boot.device.handler;
//
//import com.yomahub.liteflow.flow.LiteflowResponse;
//import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
//import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
//import com.youlai.boot.core.ruleEngine.FlowLogExecutor;
//import com.youlai.boot.core.ruleEngine.SceneContext;
//import com.youlai.boot.device.model.entity.Device;
//import com.youlai.boot.scene.model.entity.Scene;
//import com.youlai.boot.scene.model.entity.SceneDevice;
//import com.youlai.boot.scene.service.SceneDeviceService;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * 场景规则处理器
// * 用于处理MQTT消息接收后的场景交互和规则触发
// *
// * @author your-name
// */
//@Service
//@Slf4j
//public class SceneRuleHandler {
//
//    @Autowired
//    private SceneDeviceService sceneDeviceService;
//
//    @Autowired
//    private CategoryDeviceRelationshipService categoryDeviceRelationshipService;
//
//    @Resource
//    private FlowLogExecutor flowLogExecutor;
//
//    /**
//     * 处理设备上报属性数据并触发相关场景规则
//     *
//     * @param report 上报的数据信息
//     */
//    public void handlePropertyReport(Device report) {
//        try {
//            // 查询设备关联的所有启用场景
//            SceneDevice sceneDevice = new SceneDevice();
//            sceneDevice.setSerialNumber(report.getDeviceCode());
//            //查分类
//            CategoryDeviceRelationship reRelationship = categoryDeviceRelationshipService.getByDeviceId(report.getId());
//            sceneDevice.setCategoryId(reRelationship.getCategoryId());
//            List<Scene> scenes = sceneDeviceService.selectTriggerDeviceRelateScenes(sceneDevice);
//            if (scenes != null && !scenes.isEmpty()) {
//                // 创建场景上下文
//                SceneContext sceneContext = new SceneContext(
//                        report.getDeviceCode(),
//                        reRelationship.getCategoryId(),
//                        1, // 1=属性上报
//                        report.getDeviceInfo()
//                );
//
//                // 执行每个场景的规则链
//                for (Scene scene : scenes) {
//                    if (scene.getChainName() != null) {
//                        String requestId = "scene/" + scene.getId();
//                        LiteflowResponse response = flowLogExecutor.execute2RespWithRid(
//                                scene.getChainName(),
//                                null,
//                                requestId,
//                                sceneContext
//                        );
//
//                        if (!response.isSuccess()) {
//                            log.error("场景规则执行发生错误：{}", response.getMessage());
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("处理场景规则时发生异常：", e);
//        }
//    }
//
//    /**
//     * 处理设备事件上报并触发相关场景规则
//     *
//     * @param report 上报的事件信息
//     */
//    public void handleEventReport(Device report) {
//        try {
//            // 查询设备关联的所有启用场景
//            SceneDevice sceneDevice = new SceneDevice();
//            sceneDevice.setSerialNumber(report.getDeviceCode());
//            //查分类
//            CategoryDeviceRelationship reRelationship = categoryDeviceRelationshipService.getByDeviceId(report.getId());
//            sceneDevice.setCategoryId(reRelationship.getCategoryId());
//            List<Scene> scenes = sceneDeviceService.selectTriggerDeviceRelateScenes(sceneDevice);
//            if (scenes != null && !scenes.isEmpty()) {
//                // 创建场景上下文（事件类型为3）
//                SceneContext sceneContext = new SceneContext(
//                        report.getDeviceCode(),
//                        reRelationship.getCategoryId(),
//                        3, // 3=事件上报
//                        (report.getDeviceInfo())
//                );
//
//                // 执行每个场景的规则链
//                for (Scene scene : scenes) {
//                    if (scene.getChainName() != null) {
//                        String requestId = "scene/" + scene.getId();
//                        LiteflowResponse response = flowLogExecutor.execute2RespWithRid(
//                                scene.getChainName(),
//                                null,
//                                requestId,
//                                sceneContext
//                        );
//
//                        if (!response.isSuccess()) {
//                            log.error("场景规则执行发生错误：{}", response.getMessage());
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("处理事件场景规则时发生异常：", e);
//        }
//    }
//
////    /**
////     * 处理设备上线事件并触发相关场景规则
////     *
////     * @param serialNumber 设备序列号
////     * @param productId    产品ID
////     */
////    public void handleDeviceOnline(String serialNumber, Long productId) {
////        try {
////            // 查询设备关联的所有启用场景
////            SceneDevice sceneDevice = new SceneDevice();
////            sceneDevice.setSerialNumber(serialNumber);
////            //查分类
////            CategoryDeviceRelationship reRelationship = categoryDeviceRelationshipService.getByDeviceId(report.getId());
////            sceneDevice.setCategoryId(reRelationship.getCategoryId());
////            List<Scene> scenes = sceneDeviceService.selectTriggerDeviceRelateScenes(sceneDevice);
////            if (scenes != null && !scenes.isEmpty()) {
////                // 创建场景上下文（设备上线类型为5）
////                SceneContext sceneContext = new SceneContext(
////                        serialNumber,
////                        productId,
////                        5, // 5=设备上线
////                        null
////                );
////
////                // 执行每个场景的规则链
////                for (Scene scene : scenes) {
////                    if (scene.getChainName() != null) {
////                        String requestId = "scene/" + scene.getId();
////                        LiteflowResponse response = flowLogExecutor.execute2RespWithRid(
////                                scene.getChainName(),
////                                null,
////                                requestId,
////                                sceneContext
////                        );
////
////                        if (!response.isSuccess()) {
////                            log.error("场景规则执行发生错误：{}", response.getMessage());
////                        }
////                    }
////                }
////            }
////        } catch (Exception e) {
////            log.error("处理设备上线场景规则时发生异常：", e);
////        }
////    }
//
////    /**
////     * 处理设备下线事件并触发相关场景规则
////     *
////     * @param serialNumber 设备序列号
////     * @param productId    产品ID
////     */
////    public void handleDeviceOffline(String serialNumber, Long productId) {
////        try {
////            // 查询设备关联的所有启用场景
////            SceneDevice sceneDevice = new SceneDevice();
////            sceneDevice.setSerialNumber(serialNumber);
////            //查分类
////            CategoryDeviceRelationship reRelationship = categoryDeviceRelationshipService.getByDeviceId(report.getId());
////            sceneDevice.setCategoryId(reRelationship.getCategoryId());
////            List<Scene> scenes = sceneDeviceService.selectTriggerDeviceRelateScenes(sceneDevice);
////
////            if (scenes != null && !scenes.isEmpty()) {
////                // 创建场景上下文（设备下线类型为6）
////                SceneContext sceneContext = new SceneContext(
////                        serialNumber,
////                        productId,
////                        6, // 6=设备下线
////                        null
////                );
////
////                // 执行每个场景的规则链
////                for (Scene scene : scenes) {
////                    if (scene.getChainName() != null) {
////                        String requestId = "scene/" + scene.getId();
////                        LiteflowResponse response = flowLogExecutor.execute2RespWithRid(
////                                scene.getChainName(),
////                                null,
////                                requestId,
////                                sceneContext
////                        );
////
////                        if (!response.isSuccess()) {
////                            log.error("场景规则执行发生错误：{}", response.getMessage());
////                        }
////                    }
////                }
////            }
////        } catch (Exception e) {
////            log.error("处理设备下线场景规则时发生异常：", e);
////        }
////    }
////
////    /**
////     * 解析事件消息为ThingsModelSimpleItem列表
////     *
////     * @param message 事件消息
////     * @return ThingsModelSimpleItem列表
////     */
////    private List<ThingsModelSimpleItem> parseEventItems(String message) {
////        try {
////            return com.alibaba.fastjson2.JSON.parseArray(message, ThingsModelSimpleItem.class);
////        } catch (Exception e) {
////            log.error("解析事件消息时发生异常：", e);
////            return null;
////        }
////    }
//}