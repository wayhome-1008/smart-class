package com.youlai.boot.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *@Author: way
 *@CreateTime: 2025-06-05  10:04
 *@Description: TODO
 */
@Data
@ConfigurationProperties(prefix = "influx")
public class InfluxDBProperties {
    private String url;
    private String token;
    private String org;
    private String bucket;
    private int connectTimeout;
    private int readTimeout;
    private int writeTimeout;
}
