//package com.youlai.boot.config.mqtt;
//
//import com.google.common.collect.Maps;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//import java.util.Map;
//
///**
// * 消息处理类加载器
// */
//@Component
//public class MsgHandlerContextImp implements ApplicationContextAware, MsgHandlerContext {
//    private ApplicationContext context;
//    private Map<String, MsgHandler> handlerMap = Maps.newHashMap();
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        context = applicationContext;
//    }
//    @PostConstruct
//    public void init() {
//        Map<String, MsgHandler> map = context.getBeansOfType(MsgHandler.class);
////        String[] beanNamesForType = context.getBeanNamesForType(MsgHandler.class);
////        Arrays.asList(beanNamesForType).forEach(beanName -> {
////            MsgHandler signService = (MsgHandler) context.getBean(beanName);
////            handlerMap.put("/zbgw/9454c5ee8180/register", signService);
////        });
//        map.values().stream().forEach(v -> {
//            String topic = v.getClass().getAnnotation(Topic.class).value();
//            handlerMap.put(topic, v);
//            System.out.println("注册 Topic: " + topic);
//        });
//    }
//    public MsgHandler getMsgHandler(String msgType) {
//        return handlerMap.get(msgType);
//    }
//}
