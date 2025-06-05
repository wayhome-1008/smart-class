package com.youlai.boot.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.youlai.boot.config.property.InfluxDBProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 *@Author: way
 *@CreateTime: 2025-06-05  10:04
 *@Description: TODO
 */
@Configuration
@Slf4j
public class InfluxDBConfig implements DisposableBean {
    private final InfluxDBProperties properties;
    private volatile InfluxDBClient clientInstance;

    @Autowired
    public InfluxDBConfig(InfluxDBProperties properties) {
        this.properties = properties;
    }

    @Bean
    public InfluxDBClient influxDBClient() {
        validateConfiguration();

        if (clientInstance == null) {
            clientInstance = createInfluxDBClient();
        }

        return clientInstance;
    }

    private InfluxDBClient createInfluxDBClient() {
        OkHttpClient customClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .build();

        InfluxDBClientOptions options = InfluxDBClientOptions.builder()
                .url(properties.getUrl())
                .authenticateToken(properties.getToken().toCharArray())
                .org(properties.getOrg())
                .bucket(properties.getBucket())
                .okHttpClient(customClient.newBuilder())
                .build();

        return InfluxDBClientFactory.create(options);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getUrl())) {
            throw new IllegalArgumentException("InfluxDB URL 未配置");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            throw new IllegalArgumentException("InfluxDB Token 未配置");
        }
        if (!StringUtils.hasText(properties.getOrg())) {
            throw new IllegalArgumentException("InfluxDB Org 未配置");
        }
        if (!StringUtils.hasText(properties.getBucket())) {
            throw new IllegalArgumentException("InfluxDB Bucket 未配置");
        }
    }

    @Override
    public void destroy() {
        if (clientInstance != null) {
            clientInstance.close();
            log.info("InfluxDB 客户端已安全关闭");
        }
    }
}
