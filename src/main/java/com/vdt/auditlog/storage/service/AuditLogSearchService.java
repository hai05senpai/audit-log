package com.vdt.auditlog.storage.service;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import com.vdt.auditlog.storage.dto.SearchLogRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;

import java.util.stream.Collectors;

@Service
public class AuditLogSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public Object dynamicSearchLogs(SearchLogRequest request) {
        // Khởi tạo Builder cho BoolQuery để kết hợp filter động
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 1. Full-text Search theo từ khóa tự do
        // CẢI TIẾN: Chỉ quét trên queryStatement và actor vì queryStatement hiện tại đã chứa trọn vẹn SQL thô
        if (StringUtils.hasText(request.getQuery())) {
            boolQueryBuilder.must(m -> m.multiMatch(mm -> mm
                    .query(request.getQuery())
                    .fields("queryStatement", "actor") 
            ));
        }

        // 2. Filter chính xác theo dbType
        if (StringUtils.hasText(request.getDbType())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("dbType")
                    .value(request.getDbType().toUpperCase())
            ));
        }

        // 3. Filter chính xác theo actionType
        if (StringUtils.hasText(request.getActionType())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("actionType")
                    .value(request.getActionType().toUpperCase())
            ));
        }

        // 4. Filter động theo khoảng thời gian từ ngày (fromDate) đến ngày (toDate)
        if (request.getFromDate() != null || request.getToDate() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> r
                .untyped(u -> {
                    u.field("timestamp");
                    if (request.getFromDate() != null) {
                        u.gte(JsonData.of(request.getFromDate()));
                    }
                    if (request.getToDate() != null) {
                        u.lte(JsonData.of(request.getToDate()));
                    }
                    return u;
                })
            ));
        }

        // 5. Cấu hình phân trang và Sort (Mặc định log mới nhất lên đầu)
        Pageable pageable = PageRequest.of(
                request.getPage(), 
                request.getSize(), 
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // 6. Đóng gói vào NativeQuery
        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(new Query(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        // 7. Thực thi tìm kiếm trên Elasticsearch
        SearchHits<AuditLogEvent> searchHits = elasticsearchOperations.search(searchQuery, AuditLogEvent.class);

        // Tự tính toán các thông số phân trang từ searchHits
        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // Trả về map kết quả phân trang chuẩn
        return java.util.Map.of(
                "content", searchHits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList()),
                "page", pageable.getPageNumber(),
                "size", pageable.getPageSize(),
                "totalElements", totalElements,
                "totalPages", totalPages
        );
    }
}