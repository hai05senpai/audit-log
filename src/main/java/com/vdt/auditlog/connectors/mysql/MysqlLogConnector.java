package com.vdt.auditlog.connectors.mysql;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.vdt.auditlog.connectors.base.LogConnector;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
public class MysqlLogConnector implements LogConnector {

    private final BinaryLogClient client;
    private final String connectorName;
    private boolean running = false;

    public MysqlLogConnector(String host, int port, String username, String password, String connectorName) {
        this.connectorName = connectorName;
        // Khởi tạo client kết nối tới MySQL để đọc Binlog
        this.client = new BinaryLogClient(host, port, username, password);
        
        // Đăng ký bộ lắng nghe sự kiện (Event Listener)
        this.client.registerEventListener(this::handleBinlogEvent);
    }

    private void handleBinlogEvent(Event event) {
        EventHeader header = event.getHeader();
        EventType eventType = header.getEventType();

        // Chỉ lọc các sự kiện thay đổi dữ liệu (Write, Update, Delete) ở định dạng ROW
        if (EventType.isWrite(eventType) || EventType.isUpdate(eventType) || EventType.isDelete(eventType)) {
            log.info("[{}] Phát hiện sự kiện thay đổi dữ liệu: {}", connectorName, eventType);
            
            long nextPosition = ((EventHeaderV4) header).getNextPosition();
            String binlogFilename = client.getBinlogFilename();
            
            // TODO: Gửi dữ liệu thô (raw data) này vào Kafka Producer tại đây
            log.debug("Checkpoint hiện tại: File={}, Position={}", binlogFilename, nextPosition);
        }
    }

    @Override
    public void start() throws Exception {
        if (!running) {
            log.info("Đang khởi động MySQL Connector: {}", connectorName);
            running = true;
            // Chạy client trên một luồng riêng biệt để không block ứng dụng chính
            new Thread(() -> {
                try {
                    client.connect();
                } catch (IOException e) {
                    log.error("Lỗi kết nối MySQL Binlog tại connector {}", connectorName, e);
                    running = false;
                }
            }).start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (running) {
            log.info("Đang dừng MySQL Connector: {}", connectorName);
            client.disconnect();
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return this.running && client.isConnected();
    }

    @Override
    public String getConnectorName() {
        return this.connectorName;
    }
}