package com.youlai.boot.device.model.form;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *@Author: way
 *@CreateTime: 2025-04-24  09:43
 *@Description: TODO
 */
@Setter
@Getter
public class AddSubDeviceRsp {
    private int error;
    private int sequence;
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
        private String mac;

    }
}
