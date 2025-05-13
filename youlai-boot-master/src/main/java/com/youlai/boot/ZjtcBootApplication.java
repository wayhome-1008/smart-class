package com.youlai.boot;

import com.youlai.boot.config.mqtt.MqttProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * 应用启动类
 *
 * @author way
 */
@SpringBootApplication()
@ConfigurationPropertiesScan // 开启配置属性绑定
@EnableConfigurationProperties(MqttProperties.class)
public class ZjtcBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZjtcBootApplication.class, args);
    }

}
