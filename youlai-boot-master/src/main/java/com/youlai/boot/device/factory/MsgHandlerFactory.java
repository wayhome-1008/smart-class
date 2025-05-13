package com.youlai.boot.device.factory;

import com.youlai.boot.device.handler.service.MsgHandler;
import com.youlai.boot.device.topic.HandlerType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-04-27  10:21
 *@Description: TODO
 */
@Component
public class MsgHandlerFactory {
    // 用于存储处理器类型和对应处理器实例的映射
    private final Map<HandlerType, MsgHandler> handlerMap = new HashMap<>();
    // 自动注入所有实现了 MsgHandler 接口的 Bean
    @Autowired
    private List<MsgHandler> handlers;
    // 在 Bean 初始化完成后执行，将所有处理器按类型存入 handlerMap
    @PostConstruct
    public void init () {
        for (MsgHandler handler : handlers) {
            handlerMap.put (handler.getType (), handler);
        }
    }
    // 根据处理器类型获取对应的处理器实例
    public MsgHandler getHandler (HandlerType type) {
        return handlerMap.get (type);
    }
}
