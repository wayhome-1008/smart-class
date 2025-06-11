package com.youlai.boot.device.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 *@Author: way
 *@CreateTime: 2025-05-28  09:34
 *@Description: 仅使用于根据设备code查询设备信息及info属性的factory使用
 */
@Data
@AllArgsConstructor
public class DeviceInfo {
    private String name;   // 统一属性名（如 "temperature", "humidity"）
    private Object value;  // 属性值（数值/布尔/字符串）

    /**
     * 从设备信息列表中根据属性名获取属性值
     * @param infos 设备信息列表
     * @param name 要查找的属性名
     * @return 属性值的Optional对象，找不到返回Optional.empty()
     */
    public static Optional<Object> getValueByName(List<DeviceInfo> infos, String name) {
        if (infos == null || name == null) {
            return Optional.empty();
        }
        return infos.stream()
                .filter(info -> name.equals(info.getName()))
                .map(DeviceInfo::getValue)
                .findFirst();
    }

    /**
     * 从设备信息列表中根据属性名获取特定类型的属性值
     * @param infos 设备信息列表
     * @param name 要查找的属性名
     * @param clazz 期望的属性值类型
     * @return 属性值的Optional对象，找不到或类型不匹配返回Optional.empty()
     */
    public static <T> Optional<T> getValueByName(List<DeviceInfo> infos, String name, Class<T> clazz) {
        return getValueByName(infos, name)
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }
//    List<DeviceInfo> deviceInfos = Arrays.asList(
//            new DeviceInfo("temperature", 25.5),
//            new DeviceInfo("humidity", 60),
//            new DeviceInfo("status", "ON")
//    );
//
//    // 1. 获取任意类型的值
//    Optional<Object> tempValue = DeviceInfo.getValueByName(deviceInfos, "temperature");
//tempValue.ifPresent(value -> System.out.println("Temperature: " + value));
//
//    // 2. 获取特定类型的值
//    Optional<Double> temp = DeviceInfo.getValueByName(deviceInfos, "temperature", Double.class);
//temp.ifPresent(t -> System.out.println("Temperature: " + t));
//
//    Optional<Integer> humidity = DeviceInfo.getValueByName(deviceInfos, "humidity", Integer.class);
//humidity.ifPresent(h -> System.out.println("Humidity: " + h));
}
