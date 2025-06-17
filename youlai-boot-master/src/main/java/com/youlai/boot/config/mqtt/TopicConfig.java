package com.youlai.boot.config.mqtt;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-25  12:07
 *@Description: TODO
 */
public class TopicConfig {
    //基本前缀
    public final static String BASE_TOPIC = "/zbgw/";

    //① 网关请求注册: 网关上线后，向服务器请求注册，服务器
    public final static String REGISTER = "/register";

    //② 网关请求注册子设备: 网关向服务器请求注册子设备
    public final static String ADD_SUB_DEVICE = "/add_subdevice";

    //③ 网关上报子设备: 网关每次上线后，向服务器同步网关本地的所有子设备
    public final static String REPORT_SUB_DEVICE = "/report_subdevice";

    //④ 子设备事件: 子设备上线、离线、被删除，场景被执行等
    public final static String EVENT = "/event";

    //⑤ 网关上报子设备状态: 子设备状态变化后，通过此主题通知服务器状态
    public final static String SUB_UPDATE = "/sub/update";
    public final static String SUB_UPDATE2 = "/sub/update";

    //⑥ 网关回应子设备控制结果: 控制设备后，网关返回控制结果
    public final static String SUB_CONTROL_RSP = "/sub/control_rsp";

    //⑦ 网关回应读取子设备状态结果: 服务器主动读取子设备状态，此命令一般无需适配，子设备会通过update主题定期上报
    public final static String SUB_ATTRIBUTE_RSP = "/sub/attribute_rsp";

    //⑧ 网关上报OTA结果: 网关上报OTA执行结果
    public final static String OTA_RSP = "/ota_rsp";

    //⑨ 网关上报执行结果: 网关上报执行结果
    public final static String SUB_THINGS_RSP = "/sub/things_rsp";

    //⑩ 网关管理执行结果: 网关上报场景执行结果
    public final static String MANAGE_RSP = "/manage_rsp";

    //⑪ 网关请求服务器: 请求时间OTA更新、请求同步场景等网关主动发起功能
    public final static String REQUEST = "/request";

    //⑫ 网关回应服务器读取子设备请求
    public final static String SUB_GET_RSP = "/sub/get_rsp";

    public final static String GIAO="/sub/manage_rsp";

    //tasmota设备
//    public final static String INFO="/tele/"
    public static List<String> TOPIC_LIST = List.of(REPORT_SUB_DEVICE, SUB_UPDATE2, REGISTER, ADD_SUB_DEVICE, EVENT, SUB_UPDATE, SUB_CONTROL_RSP, SUB_ATTRIBUTE_RSP, OTA_RSP, SUB_THINGS_RSP, MANAGE_RSP, REQUEST, SUB_GET_RSP,GIAO);




}
