package com.vdt.auditlog.storage.repository;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends ElasticsearchRepository<AuditLogEvent, String> {

    // Tìm kiếm phân trang dựa theo loại hành động và khoảng thời gian (Yêu cầu phi chức năng số 1)
    Page<AuditLogEvent> findByActionTypeAndTimestampBetween(
            String actionType, Instant start, Instant end, Pageable pageable
    );

    // Tìm kiếm lỏng lẻo (Full-text search) trên câu lệnh query hoặc payload
    Page<AuditLogEvent> findByQueryStatementContaining(String query, Pageable pageable);
}