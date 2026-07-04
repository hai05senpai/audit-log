package com.vdt.auditlog.storage.service;

import com.vdt.auditlog.normalization.model.AuditLogEvent;
import com.vdt.auditlog.storage.dto.SearchLogRequest;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<AuditLogEvent> searchAdvancedLogs(SearchLogRequest request) {
        // 1. Khởi tạo phân trang và sắp xếp giảm dần theo thời gian
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // 2. Sử dụng Builder để xây dựng Bool Query (AND các điều kiện lại với nhau)
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        List<Query> mustQueries = new ArrayList<>();

        // Điều kiện 1: Full-text search trên queryStatement (Dùng Match Query)
        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            mustQueries.add(Query.of(q -> q.match(m -> m.field("queryStatement").query(request.getQuery()))));
        }

        // Điều kiện 2: Lọc chính xác theo Actor (Dùng Term Query vì actor là Keyword)
        if (request.getActor() != null && !request.getActor().trim().isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("actor").value(request.getActor()))));
        }

        // Điều kiện 3: Lọc chính xác theo Action Type (Dùng Term Query)
        if (request.getActionType() != null && !request.getActionType().trim().isEmpty()) {
            mustQueries.add(Query.of(q -> q.term(t -> t.field("actionType").value(request.getActionType()))));
        }

        // Điều kiện 4: Lọc theo khoảng thời gian timestamp
        if (request.getFromTime() != null || request.getToTime() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            mustQueries.add(Query.of(q -> q.range(r -> r
                .date(d -> {
                    d.field("timestamp"); // Khai báo field phải nằm TRONG khối date()
                    if (request.getFromTime() != null) {
                        java.time.LocalDateTime fromDateTime = java.time.Instant.ofEpochMilli(request.getFromTime())
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                        d.gte(fromDateTime.format(formatter));
                    }
                    if (request.getToTime() != null) {
                        java.time.LocalDateTime toDateTime = java.time.Instant.ofEpochMilli(request.getToTime())
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                        d.lte(toDateTime.format(formatter));
                    }
                    return d;
                })
            )));
        }

        // Đóng gói tất cả các điều kiện có tồn tại vào bộ lọc MUST (AND)
        boolQueryBuilder.must(mustQueries);

        // 3. Tạo NativeQuery để thực thi xuống Elasticsearch
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
                .withPageable(pageable)
                .build();

        // 4. Thực thi truy vấn
        SearchHits<AuditLogEvent> searchHits = elasticsearchOperations.search(nativeQuery, AuditLogEvent.class);

        // 5. Khử bọc (unwrap) kết quả từ SearchHits sang List entity thuần túy
        List<AuditLogEvent> content = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        // 6. Trả về đối tượng Page chuẩn để Frontend dễ chia trang
        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }
}