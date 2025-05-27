package com.youlai.boot.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2024-12-20  11:22
 *@Description: TODO
 */
@RequiredArgsConstructor
public class RestUtils {
    private static final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送HTTP请求的通用方法，支持多种请求方法（GET、POST、PUT、DELETE等）
     *
     * @param url         请求的URL地址
     * @param contentType      HTTP请求方法（如 HttpMethod.GET、HttpMethod.POST等）
     * @param contentType 请求内容的类型（如 MediaType.APPLICATION_JSON）
     * @param requestBody 请求体（对于GET请求通常为null，POST、PUT等请求传入相应对象）
     * @param responseType 期望的响应类型（用于反序列化响应数据）
     * @param <T>         请求体对象的类型
     * @param <R>         响应对象的类型
     * @return 返回根据responseType解析后的响应对象
     */
    public static <T, R> R sendHttp(String url, MediaType contentType, T requestBody, Class<R> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        HttpEntity<T> requestEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(url, requestEntity, responseType);
    }

    /**
     * 发送HTTP请求的通用方法，支持多种请求方法（GET、POST、PUT、DELETE等）
     *
     * @param url         请求的URL地址
     * @param method      HTTP请求方法（如 HttpMethod.GET、HttpMethod.POST等）
     * @param contentType 请求内容的类型（如 MediaType.APPLICATION_JSON）
     * @param requestBody 请求体（对于GET请求通常为null，POST、PUT等请求传入相应对象）
     * @param responseType 期望的响应类型（用于反序列化响应数据）
     * @param <T>         请求体对象的类型
     * @param <R>         响应对象的类型
     * @return 返回根据responseType解析后的响应对象
     */
    public static <T, R> R sendHttp(String url, HttpMethod method, MediaType contentType, T requestBody, Class<R> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        HttpEntity<T> requestEntity = null;
        if (requestBody != null) {
            requestEntity = new HttpEntity<>(requestBody, headers);
        } else {
            requestEntity = new HttpEntity<>(headers);
        }
        if (method == HttpMethod.GET) {
            // 处理GET请求，对于GET请求，如果有请求参数，可以将其拼接到URL后面（这里简单示例，实际可能需要更复杂处理）
            if (requestBody instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> paramMap = (Map<String, ?>) requestBody;
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, ?> entry : paramMap.entrySet()) {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(entry.getKey()).append("=").append(entry.getValue());
                }
                if (queryString.length() > 0) {
                    url += "?" + queryString.toString();
                }
            }
            return restTemplate.getForObject(url, responseType);
        } else if (method == HttpMethod.POST) {
            return restTemplate.postForObject(url, requestEntity, responseType);
        } else if (method == HttpMethod.PUT) {
            return restTemplate.exchange(url, method, requestEntity, responseType).getBody();
        } else if (method == HttpMethod.DELETE) {
            restTemplate.delete(url);
            // 根据实际业务逻辑，决定DELETE请求返回值情况，这里简单返回null，也可返回表示删除结果的特定对象等
            return null;
        }
        throw new IllegalArgumentException("不支持的HTTP请求方法");
    }
}
