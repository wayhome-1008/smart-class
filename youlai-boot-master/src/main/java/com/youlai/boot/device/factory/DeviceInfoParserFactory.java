package com.youlai.boot.device.factory;

import com.youlai.boot.device.service.DeviceInfoParser;
import com.youlai.boot.device.service.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  09:49
 *@Description: TODO
 */
@Component
public class DeviceInfoParserFactory {
    // 存储解析器的映射（可从数据库/配置中心加载，此处用枚举示例）
    private static final Map<String, DeviceInfoParser> PARSER_MAP = new HashMap<>();

    static {
        // 注册解析器：键为 "设备类型:协议" 组合
        //ZigBee温湿度传感器
        PARSER_MAP.put("Sensor:ZigBee", new ZigBeeSensorParser());
        //ZigBee随意贴
        PARSER_MAP.put("FreePosting:ZigBee", new ZigBeeFreePostingParser());
        //ZigBee开关
        PARSER_MAP.put("Switch:ZigBee", new ZigBeeSwitchParser());
        //ZigBee计量插座
        PARSER_MAP.put("Plug:ZigBee", new ZigBeePlugParser());
        //ZigBee人体存在雷达
        PARSER_MAP.put("HumanSensor:ZigBee", new ZigBeeHumanSensorParser());
        //ZigBee人体红外雷达
        PARSER_MAP.put("HumanRadarSensor:ZigBee", new ZigBeeHumanRadarSensorParser());
        //ZigBee插座
        PARSER_MAP.put("SmartPlug:ZigBee", new ZigBeeSmartPlugParser());
        //ZigBee串口通讯设备
        PARSER_MAP.put("Switch:ZigBee/Serial", new ZigBeeSerialParser());
        //MQTT温湿度传感器
        PARSER_MAP.put("Sensor:WiFi", new MqttSensorParser());
       // MQTT开关
        PARSER_MAP.put("Switch:WiFi", new MqttLightParser());
         //MQTT3合1传感器
        PARSER_MAP.put("Sensor3On1:WiFi", new MqttSensor3On1Parser());
        //MQTT计量插座
        PARSER_MAP.put("Plug:WiFi", new MqttPlugParser());
        //MQTT空开
        PARSER_MAP.put("AirSwitch:WiFi", new MqttPlugParser());
    }

    /**
     * 根据设备类型和协议获取解析器
     * @param deviceType 设备类型（如 "温湿度传感器"）
     * @return 解析器实例
     * @throws IllegalArgumentException 无匹配解析器时抛出异常
     */
    public static DeviceInfoParser getParser(String deviceType, String protocol) {
        String key = deviceType + ":" + protocol;
        DeviceInfoParser parser = PARSER_MAP.get(key);
        if (parser == null) {
            throw new IllegalArgumentException("未找到设备类型[" + deviceType + "]协议[" + protocol + "]的解析器");
        }
        return parser;
    }
}
