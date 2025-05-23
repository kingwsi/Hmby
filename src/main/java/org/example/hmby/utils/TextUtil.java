package org.example.hmby.utils;

/**
 * @author ws </br>
 * 2025/5/15
 */
public class TextUtil {
    public static String removeXmlTag(String text, String tagName) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName is null or empty");
        }
        return text.replaceAll("(?s)<" + tagName + ">.*?</" + tagName + ">", "");
    }

    /**
     * 去除中文标点符号
     *
     * @param input 原始字符串
     * @return 去除中文符号后的字符串
     */
    public static String removeChinesePunctuation(String input) {
        if (input == null) {
            return null;
        }

        // 中文标点正则
        String chinesePunctuationRegex = "[\\p{IsPunctuation}，。！？【】、；：《》“”‘’（）《》…—]";
        return input.replaceAll(chinesePunctuationRegex, " ");
    }
}
