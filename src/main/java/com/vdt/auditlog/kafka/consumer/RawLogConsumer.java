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
    private final AuditLogRepository auditLogRepository; // Tiêm repo vào đây

    @KafkaListener(topics = "mysql-raw-logs", groupId = "audit-log-normalization-group")
    public void consumeMysqlLogs(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        // 1. Chuẩn hóa log
        AuditLogEvent unifiedLog = normalizationService.normalizeMysqlLog(message, key);
        
        // 2. Lưu trực tiếp vào Elasticsearch (At-least-once delivery)
        auditLogRepository.save(unifiedLog);
        log.info("[Pipeline] Đã thu thập, chuẩn hóa và lưu trữ log MySQL thành công! ID: {}", unifiedLog.getId());
    }

    @KafkaListener(topics = "postgres-raw-logs", groupId = "audit-log-normalization-group")
    public void consumePostgresLogs(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        // 1. Chuẩn hóa log
        AuditLogEvent unifiedLog = normalizationService.normalizePostgresLog(message, key);
        
        // 2. Lưu trực tiếp vào Elasticsearch
        auditLogRepository.save(unifiedLog);
        log.info("[Pipeline] Đã thu thập, chuẩn hóa và lưu trữ log Postgres thành công! ID: {}", unifiedLog.getId());
    }
}