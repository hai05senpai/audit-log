package com.vdt.auditlog.connectors.mysql;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.vdt.auditlog.connectors.base.LogConnector;
import com.vdt.auditlog.kafka.producer.RawLogProducer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class MysqlLogConnector implements LogConnector {

    private final BinaryLogClient client;
    private final String connectorName;
    private final RawLogProducer rawLogProducer;
    private boolean running = false;

    public MysqlLogConnector(String host, int port, String username, String password, String connectorName, RawLogProducer rawLogProducer) {
        this.connectorName = connectorName;
        this.rawLogProducer = rawLogProducer;
        this.client = new BinaryLogClient(host, port, username, password);
        this.client.registerEventListener(this::handleBinlogEvent);
    }

    private void handleBinlogEvent(Event event) {
        EventHeader header = event.getHeader();
        EventType eventType = header.getEventType();

        if (EventType.isWrite(eventType) || EventType.isUpdate(eventType) || EventType.isDelete(eventType)) {
            log.info("[{}] Phát hiện sự kiện thay đổi dữ liệu: {}", connectorName, eventType);
            
            long nextPosition = ((EventHeaderV4) header).getNextPosition();
            String binlogFilename = client.getBinlogFilename();
            
            log.debug("Checkpoint hiện tại: File={}, Position={}", binlogFilename, nextPosition);

            try {
                String rawData = event.toString(); 
                
                rawLogProducer.sendRawLog("mysql-raw-logs", connectorName, rawData); 
                
            } catch (Exception e) {
                log.error("[{}] Lỗi khi gửi dữ liệu sang Kafka Producer", connectorName, e);
            }
        }
    }

    @Override
    public void start() throws Exception {
        if (!running) {
            log.info("Đang khởi động MySQL Connector: {}", connectorName);
            running = true;
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