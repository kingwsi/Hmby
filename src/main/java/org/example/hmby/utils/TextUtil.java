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
}
