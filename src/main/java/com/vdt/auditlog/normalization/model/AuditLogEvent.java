package com.vdt.auditlog.normalization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "audit-logs")
public class AuditLogEvent {
    @Id
    private String id;              // Thêm @Id để Spring hiểu đây là khóa chính
    
    @Field(type = FieldType.Long)
    private Long timestamp;         
    
    @Field(type = FieldType.Keyword) // Đổi thành Keyword để map chính xác
    private String dbSource;        
    
    @Field(type = FieldType.Keyword) // QUAN TRỌNG: Giúp câu lệnh .term() so khớp chính xác
    private String dbType;          
    
    @Field(type = FieldType.Text, analyzer = "standard") // Cho phép full-text search lỏng lẻo
    private String actor;           
    
    @Field(type = FieldType.Keyword) // QUAN TRỌNG: Giúp câu lệnh .term() so khớp chính xác
    private String actionType;      
    
    @Field(type = FieldType.Text, analyzer = "standard") // Cho phép full-text search
    private String queryStatement;  
    
    // Khai báo Object để Elasticsearch có thể bóc tách search xuyên vào bên trong payload
    @Field(type = FieldType.Object) 
    private Map<String, Object> payload; 
}