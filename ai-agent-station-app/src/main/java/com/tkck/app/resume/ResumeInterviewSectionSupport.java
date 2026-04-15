package com.tkck.app.resume;

final class ResumeInterviewSectionSupport {

    private ResumeInterviewSectionSupport() {
    }

    static String extractSectionValue(String text, String... markers) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replace("\r\n", "\n");
        for (String marker : markers) {
            int start = normalized.indexOf(marker);
            if (start < 0) {
                continue;
            }
            String tail = normalized.substring(start + marker.length()).trim();
            StringBuilder value = new StringBuilder();
            for (String rawLine : tail.split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    if (!value.isEmpty()) {
                        break;
                    }
                    continue;
                }
                if (!value.isEmpty() && looksLikeNewSection(line)) {
                    break;
                }
                if (!value.isEmpty()) {
                    value.append("\n");
                }
                value.append(line);
            }
            if (!value.isEmpty()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private static boolean looksLikeNewSection(String line) {
        return line.endsWith(":")
                || line.endsWith("：")
                || line.startsWith("本轮评分")
                || line.startsWith("本轮点评")
                || line.startsWith("优势")
                || line.startsWith("薄弱点")
                || line.startsWith("命中简历片段")
                || line.startsWith("追问意图")
                || line.startsWith("下一轮问题")
                || line.startsWith("下一题")
                || line.startsWith("最终面试总结")
                || line.startsWith("最终总结")
                || line.startsWith("面试状态");
    }
}
