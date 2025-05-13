package com.youlai.boot.device.model.form;
import lombok.Getter;
import lombok.Setter;
/**
 *@Author: way
 *@CreateTime: 2025-04-24  12:30
 *@Description: TODO
 */
@Setter
@Getter
public class SubUpdateMeteringSocket {
    private Integer sequence;
    private Params params;
    private String deviceId;
    @Setter
    @Getter
    public static class Params {
        private String temperature;
        private String humidity;
        private String battery;
        private String Illuminance;
    }
}
