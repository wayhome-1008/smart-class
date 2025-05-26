package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *@Author: way
 *@CreateTime: 2025-05-26  17:34
 *@Description: TODO
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth-properties")
public class DeviceAuthProperties {
    private String deviceAuthUrl;
}
