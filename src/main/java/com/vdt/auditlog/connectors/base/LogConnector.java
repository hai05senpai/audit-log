package com.vdt.auditlog.connectors.base;

public interface LogConnector {
    /**
     * Khởi động kết nối và bắt đầu lắng nghe log theo thời gian thực (Non-blocking hoặc chạy trên Thread riêng)
     */
    void start() throws Exception;

    /**
     * Dừng connector một cách an toàn (Graceful shutdown), giải phóng tài nguyên kết nối
     */
    void stop() throws Exception;

    /**
     * Lấy trạng thái hiện tại của Connector
     */
    boolean isRunning();
    
    /**
     * Trả về tên định danh của Connector (ví dụ: "MySQL-Production")
     */
    String getConnectorName();
}