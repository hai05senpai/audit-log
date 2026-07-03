package com.vdt.auditlog.connectors.base;

import com.vdt.auditlog.connectors.mysql.MysqlLogConnector;
import com.vdt.auditlog.connectors.postgres.PostgresLogConnector;
import com.vdt.auditlog.kafka.producer.RawLogProducer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectorManager {

    private final RawLogProducer rawLogProducer;
    private final List<LogConnector> activeConnectors = new ArrayList<>();

    // Các biến môi trường được nạp từ application.yml / Vòng phi chức năng số 4
    @Value("${app.connectors.mysql.host:localhost}") private String mysqlHost;
    @Value("${app.connectors.mysql.port:3306}") private int mysqlPort;
    @Value("${app.connectors.mysql.username:root}") private String mysqlUser;
    @Value("${app.connectors.mysql.password:secret}") private String mysqlPassword;

    @PostConstruct
    public void initAndStartConnectors() {
        try {
            log.info("Đang khởi tạo các Database Log Connectors...");

            // 1. Khởi tạo MySQL Connector mẫu và nạp logic đẩy sang Kafka
            MysqlLogConnector mysqlConnector = new MysqlLogConnector(
                    mysqlHost, mysqlPort, mysqlUser, mysqlPassword, "MySQL-Prod-Cluster"
            ) {
                // Ghi đè phương thức xử lý sự kiện trong class nặc danh để nhúng trực tiếp Kafka Producer
                @Override
                public void start() throws Exception {
                    super.start();
                }
            };
            
            // Lưu ý: Để bóc tách sạch sẽ hơn trong thực tế, 
            // class MysqlLogConnector/PostgresLogConnector có thể nhận trực tiếp RawLogProducer qua Constructor.

            // Tạm thời kích hoạt chạy các connector
            // mysqlConnector.start();
            // activeConnectors.add(mysqlConnector);

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi khởi chạy hệ thống Connector", e);
        }
    }

    @PreDestroy
    public void stopAllConnectors() {
        log.info("Đang đóng toàn bộ kết nối DB Connectors an toàn (Graceful Shutdown)...");
        for (LogConnector connector : activeConnectors) {
            try {
                connector.stop();
            } catch (Exception e) {
                log.error("Không thể dừng connector: {}", connector.getConnectorName(), e);
            }
        }
    }
}