package com.youlai.boot.deviceJob.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务日志表单对象
 *
 * @author way
 * @since 2025-07-25 10:58
 */
@Getter
@Setter
@Schema(description = "任务日志表单对象")
public class DeviceJobLogForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
