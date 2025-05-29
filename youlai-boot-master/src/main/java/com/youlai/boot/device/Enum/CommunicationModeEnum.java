package com.youlai.boot.device.Enum;

public enum CommunicationModeEnum {
    ZIGBEE(1L, "ZigBee"),
    WIFI(2L, "WiFi"),
    TCP_IP(3L, "TCP/IP"),
    MQTT(4L, "MQTT");

    private final Long id;
    private final String name;

    CommunicationModeEnum(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getNameById(Long id) {
        for (CommunicationModeEnum mode : values()) {
            if (mode.id.equals(id)) {
                return mode.name;
            }
        }
        return "Unknown";
    }
}
