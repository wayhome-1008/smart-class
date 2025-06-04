package com.youlai.boot.common.util;

/**
 *@Author: way
 *@CreateTime: 2025-04-25  17:36
 *@Description: TODO
 */
public class MacUtils {
    /**
     * 将带有冒号的字符串去除冒号（如 "94:54:c5:ee:81:80" -> "9454c5ee8180"）
     * @param source 带冒号的原始字符串
     * @return 去除冒号后的字符串
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static String reParseMACAddress(String source) {
        // 参数校验
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("输入字符串不能为 null 或空");
        }

        // 替换掉所有的冒号
        return source.replace(":", "");
    }

    /**
     * 将字符串按每两位添加冒号的格式转换（如 "9454c5ee8180" -> "94:54:c5:ee:81:80"）
     * @param source 原始字符串（需为偶数长度的非空字符串）
     * @return 格式化后的字符串
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static String parseMACAddress(String source) {
        // 参数校验
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("输入字符串不能为 null 或空");
        }
        if (source.length() % 2 != 0) {
            throw new IllegalArgumentException("输入字符串长度必须为偶数（当前长度：" + source.length() + "）");
        }

        // 构建结果
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length(); i += 2) {
            // 截取两位字符
            String part = source.substring(i, i + 2);
            result.append(part);
            // 非最后一段时添加冒号
            if (i != source.length() - 2) {
                result.append(":");
            }
        }
        return result.toString();
    }

    // 截取原字符串的 /zbgw/9454c5ee8180 部分，留下剩余部分
    public static String removeZbgwMacPart(String input) {
        try {
            int index = input.indexOf("/zbgw/");
            if (index != -1) {
                int nextSlashIndex = input.indexOf("/", index + "/zbgw/".length());
                if (nextSlashIndex != -1) {
                    return input.substring(nextSlashIndex);
                }
            }
            return input;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从类似 "tele/aaaa/bbbb" 的路径格式中提取第二段路径（索引为1）
     * @param path 输入的路径字符串
     * @return 提取的第二段路径值，如果无法提取则返回空字符串
     */
    public static String getCodeByTopic(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 使用斜杠分割字符串
        String[] parts = path.split("/");

        // 检查数组长度是否足够
        if (parts.length < 2) {
            return "";
        }

        // 返回第二段路径（索引为1）
        return parts[1];
    }
    /**
     * 方案一：基于字符串索引提取
     * @param url 输入字符串（如：/zbgw/9454c5ee7c68/add_subdevice）
     * @return 提取的目标字符串（如：9454c5ee7c68），无匹配返回null
     */
    public static String extractFromTopic(String url) {
        String prefix = "/zbgw/";
        int prefixStart = url.indexOf(prefix);
        if (prefixStart == -1) {
            throw new IllegalArgumentException("错误topic");
        }

        // 计算/zbgw/的结束位置（prefixStart + 前缀长度）
        int contentStart = prefixStart + prefix.length();
        // 找到下一个/的位置（从contentStart开始查找）
        int contentEnd = url.indexOf("/", contentStart);
        if (contentEnd == -1) {
            throw new IllegalArgumentException("错误topic");
        }

        // 截取中间的目标字符串
        return url.substring(contentStart, contentEnd);
    }
}
