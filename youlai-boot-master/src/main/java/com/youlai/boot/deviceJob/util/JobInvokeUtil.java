package com.youlai.boot.deviceJob.util;

import com.alibaba.fastjson.JSON;
import com.youlai.boot.device.controller.DeviceOperateController;
import com.youlai.boot.device.model.form.DeviceOperate;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;

import java.util.List;

/**
 * 任务执行工具
 *
 * @author kerwincui
 */
public class JobInvokeUtil {

    /**获取消息推送接口*/
    private static DeviceOperateController messagePublish = com.youlai.boot.common.util.SpringUtils.getBean(DeviceOperateController.class);

    /**
     * 执行方法
     *
     * @param deviceJob 系统任务
     */
    public static void invokeMethod(DeviceJob deviceJob) throws Exception {
        System.out.println("------------------------执行定时任务-----------------------------");
        if (deviceJob.getJobType() == 1) {
//          // 统一使用 parseArray 处理
            List<DeviceOperate> deviceOperates = JSON.parseArray(deviceJob.getActions(), DeviceOperate.class);
            for (DeviceOperate deviceOperate : deviceOperates) {
                // 发布功能
                messagePublish.operate(deviceOperate, deviceJob.getDeviceId());
            }
        }
        // 发布属性
//            if (propertys.size() > 0) {
//                messagePublish.publishProperty(deviceJob.getProductId(), deviceJob.getSerialNumber(), propertys, 0);
//            }

//
//        } else if (deviceJob.getJobType() == 2) {
//
//        } else if (deviceJob.getJobType() == 3) {
//            System.out.println("------------------------定时执行场景联动-----------------------------");
//            List<Action> actions = JSON.parseArray(deviceJob.getActions(), Action.class);
//            for (int i = 0; i < actions.size(); i++) {
//                ThingsModelSimpleItem model = new ThingsModelSimpleItem();
//                model.setId(actions.get(i).getId());
//                model.setValue(actions.get(i).getValue());
//                model.setRemark("场景联动定时触发");
//                if (actions.get(i).getType() == 1) {
//                    List<ThingsModelSimpleItem> propertys = new ArrayList<>();
//                    propertys.add(model);
//                    messagePublish.publishProperty(actions.get(i).getProductId(), actions.get(i).getSerialNumber(), propertys, 0);
//                } else if (actions.get(i).getType() == 2) {
//                    List<ThingsModelSimpleItem> functions = new ArrayList<>();
//                    functions.add(model);
//                    messagePublish.publishFunction(actions.get(i).getProductId(), actions.get(i).getSerialNumber(), functions, 0);
//                }
//            }
//        }
    }
}
