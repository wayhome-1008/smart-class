package com.youlai.boot.alertEvent.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 报警记录表单对象
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Getter
@Setter
@Schema(description = "报警记录表单对象")
public class AlertEventForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


}
