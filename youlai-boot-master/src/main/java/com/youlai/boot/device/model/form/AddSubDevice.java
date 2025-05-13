package com.youlai.boot.device.model.form;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-24  10:01
 *@Description: TODO
 */
@Setter
@Getter
public class AddSubDevice {
    private Integer sequence;
    private Params params;

    @Setter
    @Getter
    public static class Params {
        private List<SubDevices> subDevices;

    }

    @Setter
    @Getter
    public static class SubDevices {
        private String mac;
        private int uiid;
        private Boolean online;
        private String modelId;

    }
}
