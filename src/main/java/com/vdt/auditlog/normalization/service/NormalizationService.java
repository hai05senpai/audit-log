package com.vdt.auditlog.normalization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.auditlog.normalization.model.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NormalizationService {

    private final ObjectMapper objectMapper;

    /**
     * CHUẨN HÓA LOG MYSQL: Phù hợp 100% với cấu trúc JSON phẳng từ LogSimulationRunner
     */
    public AuditLogEvent normalizeMysqlLog(String sourceOrTopic, String rawMessage) {
        AuditLogEvent event = new AuditLogEvent();
        event.setDbType("MYSQL");
        event.setActor("mysql_binlog_listener");
        
        try {
            JsonNode rootNode = objectMapper.readTree(rawMessage);
            
            // 1. Bóc tách dbSource và Timestamp
            event.setDbSource(sourceOrTopic); 
            if (rootNode.has("timestamp")) {
                String timestampStr = rootNode.get("timestamp").asText();
                // Parse chuỗi ISO-8601 (từ Instant.toString()) sang LocalDateTime
                Instant instant = Instant.parse(timestampStr);
                event.setTimestamp(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            } else {
                event.setTimestamp(LocalDateTime.now());
            }

            // 2. Bóc tách Action và map sang định dạng
            if (rootNode.has("action")) {
                String action = rootNode.get("action").asText().toUpperCase();
                if (action.matches("INSERT|UPDATE|DELETE")) {
                    event.setActionType("DML");
                } else if (action.matches("SELECT")) {
                    event.setActionType("DQL");
                } else if (action.matches("ALTER|DROP|CREATE|TRUNCATE")) {
                    event.setActionType("DDL");
                } else {
                    event.setActionType(action);
                }
            } else {
                event.setActionType("UNKNOWN");
            }
            
            // 3. Bóc tách câu lệnh Query thực tế
            if (rootNode.has("query")) {
                event.setQueryStatement(rootNode.get("query").asText());
            } else {
                event.setQueryStatement("UNKNOWN event captured from binlog");
            }

            // 4. Lưu vết payload bổ sung thông tin tên table
            String tableName = rootNode.has("table") ? rootNode.get("table").asText() : "unknown";
            event.setPayload(Map.of("table_name", tableName, "raw_message", rawMessage));

        } catch (Exception e) {
            log.error("Loi parse log MySQL: {}", e.getMessage());
            event.setTimestamp(LocalDateTime.now());
            event.setDbSource(sourceOrTopic);
            event.setActionType("UNKNOWN");
            event.setQueryStatement("UNKNOWN event captured from binlog");
        }
        return event;
    }

    /**
     * CHUẨN HÓA LOG POSTGRESQL: Sửa triệt để lỗi hoán đổi vị trí biến và gán ActionType thích hợp
     */
    public AuditLogEvent normalizePostgresLog(String dbSource, String rawQuery) {
        AuditLogEvent event = new AuditLogEvent();
        event.setDbType("POSTGRESQL");
        event.setActor("postgres_wal_streamer");
        event.setTimestamp(LocalDateTime.now()); // Sửa thành LocalDateTime.now() thay vì chuỗi Instant

        event.setDbSource(dbSource);
        event.setQueryStatement(rawQuery);
        event.setPayload(Map.of("raw_wal_summary", rawQuery != null ? rawQuery : ""));
        
        if (rawQuery == null) {
            event.setActionType("UNKNOWN");
            return event;
        }

        String cleanQuery = rawQuery.trim().toUpperCase();
        
        if (cleanQuery.contains("INSERT INTO") || 
            cleanQuery.contains("UPDATE ") || 
            cleanQuery.contains("DELETE FROM")) {
            
            event.setActionType("DML");
        } else if (cleanQuery.contains("SELECT ")) {
            event.setActionType("DQL");
        } else if (cleanQuery.contains("DROP") || 
                   cleanQuery.contains("ALTER") || 
                   cleanQuery.contains("CREATE TABLE")) {
            
            event.setActionType("DDL");
        } else {
            event.setActionType("OTHER");
        }
        
        return event;
    }
}