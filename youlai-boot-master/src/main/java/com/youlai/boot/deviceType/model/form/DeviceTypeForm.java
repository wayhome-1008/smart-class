package com.youlai.boot.deviceType.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

/**
 * 设备类型字典(自维护)表单对象
 *
 * @author way
 * @since 2025-05-19 10:59
 */
@Getter
@Setter
@Schema(description = "设备类型字典(自维护)表单对象")
public class DeviceTypeForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
