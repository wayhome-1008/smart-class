package com.youlai.boot.config;

import com.youlai.boot.device.service.impl.BatchUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 *@Author: way
 *@CreateTime: 2025-06-23  16:09
 *@Description: TODO
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationShutdownHook implements CommandLineRunner {
    private final BatchUpdate batchUpdate;

    @Override
    public void run(String... args) {
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("应用正在关闭，正在将Redis中的数据写入数据库...");
            batchUpdate.batchUpdateToDatabase();
            log.info("数据写入完成，应用关闭");
        }));
    }
}
