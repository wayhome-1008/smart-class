package com.youlai.boot.common.util;

/**
 *@Author: way
 *@CreateTime: 2025-04-25  17:36
 *@Description: TODO
 */
public class StrUtils {
    // 从字符串中提取并格式化为正确的 Mac 地址
    public static String extractAndFormatMacAddress(String input) {
        String[] parts = input.split("/");
        for (String part : parts) {
            if (part.length() == 12 && part.matches("[0-9a-fA-F]+")) {
                StringBuilder mac = new StringBuilder();
                for (int i = 0; i < part.length(); i += 2) {
                    if (i > 0) {
                        mac.append(":");
                    }
                    mac.append(part, i, i + 2);
                }
                return mac.toString();
            }
        }
        return null;
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
}
