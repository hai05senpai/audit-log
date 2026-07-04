package com.vdt.auditlog.storage.dto;

import lombok.Data;

@Data
public class SearchLogRequest {
    // Các tham số tìm kiếm nâng cao (Mọi trường đều là Optional)
    private String query;       // Tìm kiếm full-text trong queryStatement
    private String actor;       // Lọc chính xác theo người thực hiện
    private String actionType;  // Lọc chính xác theo hành động (INSERT, UPDATE, DELETE...)
    private Long fromTime;      // Từ thời gian (Epoch millisecond)
    private Long toTime;        // Đến thời gian (Epoch millisecond)

    // Tham số phân trang (Có giá trị mặc định phòng trường hợp FE không truyền)
    private int page = 0;
    private int size = 10;
}