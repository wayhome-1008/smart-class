package com.youlai.boot.config.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: mqtt配置初始化
 * @author: way
 * @date: 2024/7/22 15:03
 **/
@Configuration
@Slf4j
public class MqttConfig {
    @Autowired
    private MqttProperties mqttProperties;
    @Autowired
    private MqttCallback mqttCallback;

    @Bean
    public MqttClient mqttClient() {
        int maxRetries = 3; // 最大重试次数
        int retryInterval = 5000; // 每次重试间隔（毫秒）
        for (int i = 0; i < maxRetries; i++) {
            try {
                log.info("===初始化mqttClient====");
                // 确保客户端ID唯一
                String clientId = mqttProperties.getClient().getClientId() + "-" + System.currentTimeMillis();
                MqttClient client = new MqttClient(
                        mqttProperties.getClient().getServerUri(),
                        clientId,
                        new MemoryPersistence()
                );
                client.setManualAcks(true);
                mqttCallback.setMqttClient(client);
                client.setCallback(mqttCallback);
                client.connect(mqttConnectOptions());
                log.info("MQTT客户端连接成功 - ClientId: {}", clientId);
                return client;
            } catch (MqttException e) {
                log.error("MQTT 连接失败，尝试第 {} 次重连...", i + 1, e);
                if (i == maxRetries - 1) {
                    throw new RuntimeException("MQTT 连接失败，请检查配置", e);
                }
                try {
                    Thread.sleep(retryInterval); // 等待一段时间后重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程中断异常", ie);
                }
            }
        }
        throw new RuntimeException("MQTT客户端初始化失败");
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttProperties.getClient().getUsername());
        options.setPassword(mqttProperties.getClient().getPassword().toCharArray());
        //是否自动重新连接
        options.setAutomaticReconnect(true);
        //是否清除之前的连接信息
        options.setCleanSession(true);
        //连接超时时间
        options.setConnectionTimeout(mqttProperties.getClient().getConnectionTimeout());
        //心跳
        options.setKeepAliveInterval(mqttProperties.getClient().getKeepAliveInterval());
        //设置mqtt版本
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        // 增加最大飞行消息数
        options.setMaxInflight(1000);
        return options;
    }
}