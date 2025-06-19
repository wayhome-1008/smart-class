package com.youlai.boot.core.log;

import cn.hutool.json.JSONUtil;
import com.youlai.boot.common.enums.LogModuleEnum;
import com.youlai.boot.core.security.util.SecurityUtils;
import com.youlai.boot.system.model.entity.Log;
import com.youlai.boot.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 *@Author: way
 *@CreateTime: 2025-06-19  11:37
 *@Description: TODO
 */
@Component
@RequiredArgsConstructor
public class LogHelper {
    private final LogService logService;

    /**
     * 记录方法调用日志
     * @param module      日志模块
     * @param content     日志内容
     * @param methodName  方法名称
     * @param params      方法参数（可选）
     * @param result      方法返回值（可选）
     * @param executionTime 执行耗时（毫秒）
     */
    public void recordMethodLog(
            LogModuleEnum module,
            String content,
            String methodName,
            Object params,
            Object result,
            long executionTime
    ) {
        Log log = new Log();
        log.setModule(module);
        log.setContent(content);
        log.setExecutionTime(executionTime);

        // 设置方法信息
        log.setRequestUri("[METHOD] " + methodName);
        log.setRequestMethod("INTERNAL");

        // 设置参数和结果
        if (params != null) {
            log.setRequestParams(toStringLimited(params));
        }
        if (result != null) {
            log.setResponseContent(toStringLimited(result));
        }

        // 设置操作人
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            log.setCreateBy(userId);
        }

        // 保存日志
        logService.save(log);
    }

    /**
     * 安全转换对象为字符串（防过长）
     */
    private String toStringLimited(Object obj) {
        try {
            String json = JSONUtil.toJsonStr(obj);
            return json.length() > 2000 ? json.substring(0, 2000) + "..." : json;
        } catch (Exception e) {
            return Objects.toString(obj);
        }
    }
}
