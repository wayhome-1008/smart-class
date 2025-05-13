package com.youlai.boot.config.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @description: mqtt服务配置
 * @author: way
 * @date: 2024/7/22 15:05
 * @param:
 * @return:
 **/
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private String serverUrl;
    // getter 和 setter
    // 发布消息配置
    private Producer producer;

    // 接收订阅消息配置  
    private Consumer consumer;

    // MQTT客户端配置  
    private Client client;

    // 生产者配置  
    @Setter
    @Getter
    public static class Producer {
        // getter 和 setter
        // 质量服务
        private int defaultQos;

        // 默认同样主题仅保留最新一条消息  
        private boolean defaultRetained;

        // 默认主题  
        private String defaultTopic;

    }

    // 消费者配置  
    @Setter
    @Getter
    public static class Consumer {
        // getter 和 setter
        // 接收订阅消息主题(集合可以同时订阅多个消费者主题)
        private List<String> consumerTopics;

    }

    // MQTT客户端配置  
    @Setter
    @Getter
    public static class Client {
        // getter 和 setter
        // 用户名
        private String username;

        // 密码  
        private String password;

        // 服务器URI  
        private String serverUri;

        // 客户端ID  
        private String clientId;

        // 保持连接间隔  
        private int keepAliveInterval;

        // 连接超时时间  
        private int connectionTimeout;

    }

}