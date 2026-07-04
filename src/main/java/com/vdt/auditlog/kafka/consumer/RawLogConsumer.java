package com.vdt.auditlog.kafka.consumer;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import com.vdt.auditlog.normalization.service.NormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.vdt.auditlog.storage.repository.AuditLogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawLogConsumer {

    private final NormalizationService normalizationService;
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "mysql-raw-logs", groupId = "audit-log-normalization-group")
    public void consumeMysqlLogs(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        AuditLogEvent unifiedLog = normalizationService.normalizeMysqlLog(key, message);
        
        auditLogRepository.save(unifiedLog);
        log.info("[⚙️ MySQL Pipeline] Chuẩn hóa và lưu trữ thành công! ID: {}, Action: {}", unifiedLog.getId(), unifiedLog.getActionType());
    }

    @KafkaListener(topics = "postgres-raw-logs", groupId = "audit-log-normalization-group")
    public void consumePostgresLogs(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        AuditLogEvent unifiedLog = normalizationService.normalizePostgresLog(key, message);
        
        auditLogRepository.save(unifiedLog);
        log.info("[🐘 Postgres Pipeline] Chuẩn hóa và lưu trữ thành công! ID: {}, Action: {}", unifiedLog.getId(), unifiedLog.getActionType());
    }
}