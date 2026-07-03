package com.vdt.auditlog.storage.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class SearchLogRequest {
    private String query;         // Từ khóa tìm kiếm full-text
    private String dbType;        // Lọc theo loại DB (MYSQL/POSTGRESQL)
    private String actionType;    // Lọc theo loại hành động (CREATE/UPDATE/DELETE)
    private Instant fromDate;     // Tìm từ thời điểm nào
    private Instant toDate;       // Đến thời điểm nào
    
    // Cấu hình phân trang mặc định nếu client không truyền lên
    private int page = 0;
    private int size = 20;
}