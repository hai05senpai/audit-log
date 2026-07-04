package com.vdt.auditlog.normalization.masking;

import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataMaskingService {

    // Regex cải tiến: 
    // Group 1: Bắt key (có hoặc không có nháy) kèm dấu phân cách ( : hoặc = )
    // Group 2: Bắt dấu nháy mở (nếu có)
    // Group 3: Bắt nội dung giá trị (nếu có nháy thì dừng khi gặp nháy đóng, nếu không nháy thì dừng ở dấu phẩy, dấu ngoặc hoặc cuối dòng)
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(\"?\\b(?:password|passwd|secret|token|access_token)\\b\"?\\s*[:=]\\s*)([\"']?)([^\"',\\s}]+)(\\2)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Thay thế giá trị nhạy cảm thành ****** một cách tối ưu hiệu năng
     */
    public String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        Matcher matcher = SENSITIVE_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            // Group 1: "password": hoặc password=
            // Group 2: Dấu nháy mở ' hoặc " (nếu có)
            // Thay thế Group 3 bằng ****** và giữ nguyên Group 4 (nháy đóng nếu có)
            String replacement = matcher.group(1) + matcher.group(2) + "******" + matcher.group(4);
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}