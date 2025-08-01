package com.youlai.boot.scene.liteFlow;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.scene.model.entity.Scene;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 *@Author: way
 *@CreateTime: 2025-07-31  15:34
 *@Description: TODO
 */
@LiteflowComponent(id = "silenceCheck")
public class SilenceCheckComponent extends NodeComponent {
    // 存储场景最后执行时间（内存缓存）
    private static final ConcurrentHashMap<Long, Long> lastExecuteTime = new ConcurrentHashMap<>();

    @Override
    public void process() throws Exception {
        Scene scene = this.getContextBean(Scene.class);
        Integer silenceTime = scene.getSilenceTime();

        // 如果无需静默，直接通过
        if (silenceTime == null || silenceTime <= 0) {
            return;
        }

        // 检查是否在静默期内
        long currentTime = System.currentTimeMillis();
        long lastTime = lastExecuteTime.getOrDefault(scene.getId(), 0L);
        boolean canExecute = currentTime - lastTime > silenceTime * 60 * 1000L;

        if (canExecute) {
            lastExecuteTime.put(scene.getId(), currentTime); // 更新最后执行时间
        }
    }

    @Override
    public boolean isAccess() {
        return true; // 始终进入
    }
}
