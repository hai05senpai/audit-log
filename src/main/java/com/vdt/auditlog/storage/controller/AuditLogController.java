package com.vdt.auditlog.storage.controller;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import com.vdt.auditlog.storage.dto.SearchLogRequest;
import com.vdt.auditlog.storage.service.AuditLogService;
import co.elastic.clients.elasticsearch.ElasticsearchClient; // Thêm import này
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord; // Thêm import này
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // Mở cổng để React Frontend không bị lỗi CORS
@Tag(name = "Audit Log Search API", description = "Endpoint phục vụ tìm kiếm Full-text search nâng cao")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ElasticsearchClient esClient; // Inject client của Elastic vào đây

    @GetMapping("/search")
    @Operation(
        summary = "Tìm kiếm nâng cao tích hợp đa điều kiện", 
        description = "Hỗ trợ lọc linh hoạt kết hợp hoặc riêng lẻ giữa: khoảng thời gian, actor, actionType và full-text query."
    )
    public ResponseEntity<Page<AuditLogEvent>> searchLogs(@ParameterObject SearchLogRequest request) {
        Page<AuditLogEvent> result = auditLogService.searchAdvancedLogs(request);
        return ResponseEntity.ok(result);
    }

    // --- THÊM ENDPOINT NÀY ĐỂ FETCH INDEX CHO DATABASE UI ---
    @GetMapping("/indices")
    @Operation(summary = "Lấy danh sách các Index thực tế từ Elasticsearch cluster")
    public ResponseEntity<List<Map<String, Object>>> getElasticsearchIndices() throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        // Sử dụng API CAT Indices của thư viện mới
        List<IndicesRecord> indicesRecords = esClient.cat().indices().valueBody();

        for (IndicesRecord record : indicesRecords) {
            // Loại bỏ các index hệ thống (.kibana, .ilm-history...) cho sạch giao diện
            if (record.index() != null && record.index().startsWith(".")) {
                continue;
            }

            Map<String, Object> indexInfo = new HashMap<>();
            indexInfo.put("indexName", record.index());
            indexInfo.put("health", record.health()); // green, yellow, red
            indexInfo.put("status", record.status()); // open, close
            indexInfo.put("docCount", record.docsCount() != null ? record.docsCount() : "0");
            indexInfo.put("storeSize", record.storeSize() != null ? record.storeSize() : "0b");
            
            result.add(indexInfo);
        }

        return ResponseEntity.ok(result);
    }
}