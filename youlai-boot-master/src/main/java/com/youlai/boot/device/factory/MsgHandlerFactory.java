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
 *@Description: 根据不同类型HandlerType获取对应的处理器实例
 */
@Component
public class MsgHandlerFactory {
    @Autowired
    private List<MsgHandler> handlers;
    private final Map<HandlerType, MsgHandler> handlerMap = new HashMap<>();

    //初始化将所有处理器按类型存入handlerMap
    @PostConstruct
    public void init() {
        for (MsgHandler handler : handlers) {
            handlerMap.put(handler.getType(), handler);
        }
    }

    // 根据处理器类型获取对应的处理器实例
    public MsgHandler getHandler(HandlerType type) {
        return handlerMap.get(type);
    }
}
