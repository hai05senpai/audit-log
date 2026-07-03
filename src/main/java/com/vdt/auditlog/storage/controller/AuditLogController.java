package com.vdt.auditlog.storage.controller;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import com.vdt.auditlog.storage.dto.SearchLogRequest;
import com.vdt.auditlog.storage.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Tag(name = "Audit Log Search API", description = "Endpoint phục vụ tìm kiếm Full-text search và phân trang hệ thống Nhật ký kiểm toán")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm nâng cao và phân trang log", description = "Hỗ trợ Dataset lên tới 10 triệu bản ghi, phản hồi dưới 2 giây nhờ cấu trúc dữ liệu Elasticsearch")
    public ResponseEntity<Page<AuditLogEvent>> searchLogs(SearchLogRequest request) {
        
        Pageable pageable = PageRequest.of(
                request.getPage(), 
                request.getSize(), 
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<AuditLogEvent> result;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            result = auditLogRepository.findByQueryStatementContaining(request.getQuery(), pageable);
        } else {
            result = auditLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }
}