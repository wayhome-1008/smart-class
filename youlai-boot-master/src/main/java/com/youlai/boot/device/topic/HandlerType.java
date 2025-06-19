package com.youlai.boot.device.topic;

public enum HandlerType {
    // 1 网关请求注册: 网关上线后，向服务器请求注册，服务器
    REGISTER,
    //2 网关上报子设备: 网关每次上线后，向服务器同步网关本地的所有子设备
    REPORT_SUBDEVICE,
    //3 网关请求注册子设备: 网关向服务器请求注册子设备
    ADD_SUB_DEVICE,
    //4 子设备事件: 子设备上线、离线、被删除，场景被执行等
    EVENT,
    //5 子设备属性上报: 子设备上报属性
    SUB_UPDATE,
    //6 子设备属性下发: 服务器下发属性到子设备
    SUB_ATTRIBUTE_RSP,
    //7 子设备控制下发: 服务器下发控制到子设备
    SUB_CONTROL_RSP,
    //8 子设备OTA升级: 子设备上报属性
    OTA_RSP,
    //9 子设备管理: 子设备上报属性
    MANAGE_RSP,
    //10 子设备请求:子设备上报属性
    REQUEST,
    //11 子设备属性获取: 子设备上报属性
    SUB_GET_RSP,
    //12 子设备属性获取: 子设备上报属性
    SUB_THINGS_RSP,

    //MQTT直连设备
    SENSOR, //温湿度传感器
    LIGHT,
    STATE,
    RESULT, SENSOR3ON1,STATUS8;
}
