package com.towork.ai.util;

public final class AiJsonUtils {
    private AiJsonUtils() {}

    public static String extractJson(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = stripCodeFence(trimmed);
        }
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1).trim();
        }
        start = trimmed.indexOf('[');
        end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1).trim();
        }
        return trimmed;
    }

    private static String stripCodeFence(String content) {
        int firstNewline = content.indexOf('\n');
        if (firstNewline > -1) {
            content = content.substring(firstNewline + 1);
        }
        int fenceEnd = content.lastIndexOf("```");
        if (fenceEnd > -1) {
            content = content.substring(0, fenceEnd);
        }
        return content.trim();
    }
}
