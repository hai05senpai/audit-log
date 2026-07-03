package com.vdt.auditlog.normalization.masking;

import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataMaskingService {

    // Regex cải tiến: Hỗ trợ dấu nháy bao quanh key (JSON) và bắt chính xác value nằm trong nháy đơn/nháy kép phía sau
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"?\\b(password|passwd|secret|token|access_token)\\b\"?\\s*[:=]\\s*([\"'])(.*?)\\2", 
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Thay thế giá trị nhạy cảm thành ******
     */
    @io.swagger.v3.oas.annotations.Hidden
    public String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        Matcher matcher = SENSITIVE_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String rawValue = matcher.group(3); // Group 3 trỏ thẳng vào value nhạy cảm thô

            // Thay thế giá trị thô bằng ******
            String replacement = fullMatch.replace(rawValue, "******");
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}