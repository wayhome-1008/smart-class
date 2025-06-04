package com.youlai.boot.device.Enum;

// 设备类型枚举
public enum DeviceTypeEnum {
    GATEWAY(1L, "Gateway"),
    SENSOR(2L, "Sensor"),
    FREE_POSTING(3L, "FreePosting"),
    PLUG(4L, "Plug"),
    HUMAN_RADAR_SENSOR(5L, "HumanRadarSensor"),
    HUMAN_SENSOR(6L, "HumanSensor"),
    SWITCH(7L, "Switch"),
    LIGHT(8L, "Light"),
    SENSOR3ON1(9L, "Sensor3On1"),
    SMART_PLUG(10L, "SmartPlug");
    private final Long id;
    private final String name;

    DeviceTypeEnum(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getNameById(Long id) {
        for (DeviceTypeEnum type : values()) {
            if (type.id.equals(id)) {
                return type.name;
            }
        }
        return "Unknown";
    }
}
