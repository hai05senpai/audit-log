package com.vdt.auditlog.normalization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "audit-logs")
public class AuditLogEvent {
    
    @Id
    private String id;              
    
    // 1. Chuyển sang LocalDateTime để hiển thị đúng định dạng ngày tháng cụ thể
    // 2. Định dạng format trong ES hỗ trợ cả date_hour_minute_second thông thường
    // 3. @JsonFormat giúp Jackson tự động format chuỗi JSON trả về cho Frontend
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;         
    
    @Field(type = FieldType.Keyword) 
    private String dbSource;        
    
    @Field(type = FieldType.Keyword) 
    private String dbType;          
    
    @Field(type = FieldType.Keyword) 
    private String actor;           
    
    @Field(type = FieldType.Keyword) 
    private String actionType;      
    
    @Field(type = FieldType.Text, analyzer = "standard") 
    private String queryStatement;  
    
    @Field(type = FieldType.Flattened) 
    private Map<String, Object> payload; 
}