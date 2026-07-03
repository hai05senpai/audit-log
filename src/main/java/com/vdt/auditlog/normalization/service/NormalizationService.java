package com.vdt.auditlog.normalization.service;

import com.vdt.auditlog.normalization.masking.DataMaskingService;
import com.vdt.auditlog.normalization.model.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizationService {

    private final DataMaskingService maskingService;
    
    // Pattern tìm kiếm nội dung nằm sau cặp "query":"..." trong chuỗi JSON thô
    private static final Pattern MYSQL_QUERY_PATTERN = Pattern.compile("\"query\"\\s*:\\s*\"(.*?)\"\\s*}\\s*$");

    /**
     * Chuẩn hóa Log thô từ MySQL (Binlog JSON string)
     */
    public AuditLogEvent normalizeMysqlLog(String rawMessage, String sourceKey) {
        log.debug("Đang chuẩn hóa log MySQL từ nguồn: {}", sourceKey);
        
        // Thực hiện masking câu lệnh/dữ liệu thô trước
        String maskedMessage = maskingService.maskSensitiveData(rawMessage);
        
        Map<String, Object> mockPayload = new HashMap<>();
        mockPayload.put("raw_data_summary", maskedMessage);

        // GIẢI PHÁP: Trích xuất câu lệnh SQL thực tế ra khỏi JSON thô để Elasticsearch đánh chỉ mục text chuẩn 100%
        String cleanQueryStatement = maskedMessage;
        try {
            Matcher matcher = MYSQL_QUERY_PATTERN.matcher(maskedMessage);
            if (matcher.find()) {
                String rawQuery = matcher.group(1);
                // Khử hoàn toàn các ký tự escape dấu nháy \" thành dấy nháy đơn hoặc nháy kép thuần túy
                cleanQueryStatement = rawQuery.replace("\\\"", "\"");
            }
        } catch (Exception e) {
            log.warn("Không bóc tách được câu SQL từ log MySQL, fallback về masked message: {}", e.getMessage());
            cleanQueryStatement = maskedMessage;
        }

        return AuditLogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now().toEpochMilli())
                .dbSource(sourceKey)
                .dbType("MYSQL")
                .actor("mysql_replica_user")
                .actionType("UPDATE") 
                // queryStatement bây giờ là text thuần: INSERT INTO users ... VALUES (..., 'user10@viettel.com.vn');
                .queryStatement(cleanQueryStatement) 
                .payload(mockPayload)
                .build();
    }

    /**
     * Chuẩn hóa Log thô từ PostgreSQL (WAL/Logical Replication text string)
     */
    public AuditLogEvent normalizePostgresLog(String rawMessage, String sourceKey) {
        log.debug("Đang chuẩn hóa log Postgres từ nguồn: {}", sourceKey);

        String maskedMessage = maskingService.maskSensitiveData(rawMessage);
        
        Map<String, Object> mockPayload = new HashMap<>();
        mockPayload.put("raw_wal_summary", maskedMessage);

        return AuditLogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now().toEpochMilli())
                .dbSource(sourceKey)
                .dbType("POSTGRESQL")
                .actor("postgres_replication_client")
                .actionType("DML") 
                // Lưu toàn bộ maskedMessage nguyên vẹn vì Postgres bản chất đã là text thuần, không bị bọc JSON
                .queryStatement(maskedMessage) 
                .payload(mockPayload)
                .build();
    }
}