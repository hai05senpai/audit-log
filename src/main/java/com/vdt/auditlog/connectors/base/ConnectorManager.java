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

    // --- CONFIG MYSQL ---
    @Value("${app.connectors.mysql.host:localhost}") private String mysqlHost;
    @Value("${app.connectors.mysql.port:3306}") private int mysqlPort;
    @Value("${app.connectors.mysql.username:root}") private String mysqlUser;
    @Value("${app.connectors.mysql.password:secret}") private String mysqlPassword;

    // --- CONFIG POSTGRESQL ---
    @Value("${app.connectors.postgres.host:localhost}") private String postgresHost;
    @Value("${app.connectors.postgres.port:5432}") private int postgresPort;
    @Value("${app.connectors.postgres.database:postgres}") private String postgresDb; 
    @Value("${app.connectors.postgres.username:postgres}") private String postgresUser;
    @Value("${app.connectors.postgres.password:secret}") private String postgresPassword;
    @Value("${app.connectors.postgres.slot-name:audit_slot}") private String postgresSlotName;

    @PostConstruct
    public void initAndStartConnectors() {
        log.info("--- BẮT ĐẦU KHỞI TẠO HỆ THỐNG DB LOG CONNECTORS ---");

        // 1. Khởi chạy MySQL Connector
        try {
            log.info("Đang cấu hình MysqlLogConnector...");
            MysqlLogConnector mysqlConnector = new MysqlLogConnector(
                    mysqlHost, mysqlPort, mysqlUser, mysqlPassword, "MySQL-Prod-Cluster", rawLogProducer
            );
            mysqlConnector.start();
            activeConnectors.add(mysqlConnector);
            log.info(" Kích hoạt thành công MysqlLogConnector.");
        } catch (Exception e) {
            log.error("❌ Thất bại khi khởi chạy MysqlLogConnector", e);
        }

        // 2. Khởi chạy PostgreSQL Connector
        try {
            log.info("Đang cấu hình PostgresLogConnector...");
            PostgresLogConnector postgresConnector = new PostgresLogConnector(
                    postgresHost, postgresPort, postgresDb, postgresUser, postgresPassword, 
                    postgresSlotName, "Postgres-Core-Cluster", rawLogProducer
            );
            postgresConnector.start();
            activeConnectors.add(postgresConnector);
            log.info(" Kích hoạt thành công PostgresLogConnector.");
        } catch (Exception e) {
            log.error("❌ Thất bại khi khởi chạy PostgresLogConnector", e);
        }

        log.info("--- HOÀN THÀNH KHỞI CHẠY (Tổng số active: {}) ---", activeConnectors.size());
    }

    @PreDestroy
    public void stopAllConnectors() {
        log.info("--- ĐANG ĐÓNG TOÀN BỘ DB CONNECTORS AN TOÀN (GRACEFUL SHUTDOWN) ---");
        for (LogConnector connector : activeConnectors) {
            try {
                connector.stop();
            } catch (Exception e) {
                log.error("❌ Không thể dừng connector: {}", connector.getConnectorName(), e);
            }
        }
        activeConnectors.clear();
        log.info("--- HỆ THỐNG CONNECTOR ĐÃ DỪNG HOÀN TOÀN ---");
    }
}