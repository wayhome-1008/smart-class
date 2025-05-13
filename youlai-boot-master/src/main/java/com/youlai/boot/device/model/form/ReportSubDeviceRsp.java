package com.youlai.boot.device.model.form;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-24  11:11
 *@Description: TODO
 */
@Setter
@Getter
@ToString
public class ReportSubDeviceRsp {
    private Integer sequence;
    private Integer error;
    private Params params;

    @Setter
    @Getter
    public static class Params {
        private List<Results> results;
    }

    @Setter
    @Getter
    public static class Results {
        private int error;
        private String deviceId;
    }
}
