package com.vdt.auditlog.kafka.simulation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LogSimulationRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    // Mảng dữ liệu giả lập cho MySQL (Chỉ chứa các câu lệnh DML thao tác dữ liệu)
    private final String[] MYSQL_TABLES = {"users", "orders", "payments", "products", "customer_profiles"};
    private final String[][] MYSQL_QUERIES = {
        {"INSERT INTO users (username, password, email) VALUES ('user_%d', 'plain_pass_%d', 'user%d@viettel.com.vn');", "INSERT"},
        {"UPDATE users SET password = 'new_hashed_secret_%d', token = 'jwt_live_tok_%d' WHERE id = %d;", "UPDATE"},
        {"UPDATE orders SET total_amount = %d, status = 'COMPLETED' WHERE order_id = %d;", "UPDATE"},
        {"DELETE FROM payments WHERE payment_id = %d AND status = 'FAILED';", "DELETE"},
        {"SELECT * FROM customer_profiles WHERE id = %d AND status = 'ACTIVE';", "SELECT"}
    };

    // Mảng dữ liệu giả lập cho Postgres (Chỉ chứa các câu lệnh DML thao tác dữ liệu)
    private final String[][] POSTGRES_STATEMENTS = {
        {"LOG: statement: INSERT INTO tokens (access_token, user_id) VALUES ('secret_tok_val_%s', %d);", "INSERT"},
        {"LOG: statement: SELECT * FROM customer_profiles WHERE secret_key = 'key_vdt_2026_%s' AND status = 'ACTIVE';", "SELECT"},
        {"LOG: statement: UPDATE account_credentials SET api_password = 'pass_vng_fpt_%s' WHERE user_id = %d;", "UPDATE"}, 
        {"LOG: statement: DELETE FROM sessions WHERE session_token = 'sess_%s' OR user_id = %d;", "DELETE"},
        {"LOG: statement: INSERT INTO audit_logs (event_id, action, actor) VALUES ('evt_%s', 'LOGIN', 'user_%d');", "INSERT"}
    };

    /**
     * Tự động bắn dữ liệu giả lập DML đa dạng vào Kafka sau mỗi 5 giây
     */
    @Scheduled(fixedRate = 5000)
    public void simulateLogs() {
        // 1. Tạo và bắn Log DML cho MySQL
        simulateMysqlLog();

        // 2. Tạo và bắn Log DML cho Postgres
        simulatePostgresLog();
    }

    private void simulateMysqlLog() {
        String table = MYSQL_TABLES[random.nextInt(MYSQL_TABLES.length)];
        int queryIdx = random.nextInt(MYSQL_QUERIES.length);
        
        String action = MYSQL_QUERIES[queryIdx][1];
        String rawQueryTemplate = MYSQL_QUERIES[queryIdx][0];
        
        int val1 = random.nextInt(10000);
        int val2 = random.nextInt(1000);
        int val3 = random.nextInt(100);
        String formattedQuery = String.format(rawQueryTemplate, val1, val2, val3);

        String safeQuery = formattedQuery.replace("\"", "\\\"");
        String isoTimestamp = Instant.now().toString();

        // Đóng gói JSON payload sạch sẽ cho MySQL listener bóc tách
        String mockMysqlMessage = String.format(
            "{\"table\":\"%s\", \"action\":\"%s\", \"query\":\"%s\", \"timestamp\":\"%s\"}", 
            table, action, safeQuery, isoTimestamp
        );
        
        String mockMysqlKey = "mysql-prod-db-" + (random.nextInt(2) + 1);

        log.info("[⚙️ MySQL DML Simulator] Sending event -> Cluster: {}, Action: {}", mockMysqlKey, action);
        kafkaTemplate.send("mysql-raw-logs", mockMysqlKey, mockMysqlMessage);
    }

    private void simulatePostgresLog() {
        int statementIdx = random.nextInt(POSTGRES_STATEMENTS.length);
        String action = POSTGRES_STATEMENTS[statementIdx][1];
        String rawStatementTemplate = POSTGRES_STATEMENTS[statementIdx][0];

        String randString = UUID.randomUUID().toString().substring(0, 8);
        int randNum1 = random.nextInt(5000);
        int randNum2 = random.nextInt(200);
        
        String mockPostgresMessage = String.format(rawStatementTemplate, randString, randNum1, randNum2);
        String mockPostgresKey = "postgres-core-db-" + (random.nextInt(2) + 1);

        log.info("[🐘 Postgres DML Simulator] Sending event -> Cluster: {}, Action: {}", mockPostgresKey, action);
        kafkaTemplate.send("postgres-raw-logs", mockPostgresKey, mockPostgresMessage);
    }
}