package com.youlai.boot.device.model.form;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-24  11:01
 *@Description: TODO
 */
@Setter
@Getter
public class ReportSubDevice {
    private Integer sequence;
    private Params params;

    @Setter
    @Getter
    public static class Params {
        private Integer totalNumber;
        private Integer index;
        private List<SubDevices> subDevices;
    }

    @Setter
    @Getter
    public static class SubDevices {
        private int uiid;
        private Boolean online;
        private String deviceId;

    }
}
