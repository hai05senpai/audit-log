package com.vdt.auditlog.storage.repository;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends ElasticsearchRepository<AuditLogEvent, String> {

    // Thay đổi Instant thành LocalDateTime để đồng bộ hoàn toàn với Model AuditLogEvent
    Page<AuditLogEvent> findByActionTypeAndTimestampBetween(
            String actionType, LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    // Giữ nguyên - Tìm kiếm lỏng lẻo (Full-text search) trên câu lệnh query
    Page<AuditLogEvent> findByQueryStatementContaining(String query, Pageable pageable);
}