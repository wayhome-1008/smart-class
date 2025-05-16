package com.youlai.boot.device.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-05-16  12:24
 *@Description: TODO
 */
@Data
public class FreePostingParams {
    /**
     *
     */
    @JSONField(name = "key")
    private String key;
    /**
     *
     */
    @JSONField(name = "outlet")
    private String outlet;
}
