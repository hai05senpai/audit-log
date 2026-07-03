package com.vdt.auditlog.kafka.simulation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LogSimulationRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();

    // Mảng dữ liệu giả lập cho MySQL (Dạng JSON)
    private final String[] MYSQL_TABLES = {"users", "orders", "payments", "products", "audit_configs"};
    private final String[][] MYSQL_QUERIES = {
        {"INSERT INTO users (username, password, email) VALUES ('user_%d', 'plain_pass_%d', 'user%d@viettel.com.vn');", "INSERT"},
        {"UPDATE users SET password = 'new_hashed_secret_%d', token = 'jwt_live_tok_%d' WHERE id = %d;", "UPDATE"},
        {"UPDATE orders SET total_amount = %d, status = 'COMPLETED' WHERE order_id = %d;", "UPDATE"},
        {"DELETE FROM payments WHERE payment_id = %d AND status = 'FAILED';", "DELETE"},
        {"ALTER TABLE audit_configs ADD COLUMN custom_mask_regex VARCHAR(%d);", "ALTER"}
    };

    // Mảng dữ liệu giả lập cho Postgres (Dạng String Statement) - Đã đồng nhất cấu trúc tham số
    private final String[][] POSTGRES_STATEMENTS = {
        {"LOG: statement: INSERT INTO tokens (access_token, user_id) VALUES ('secret_tok_val_%s', %d);", "INSERT"},
        {"LOG: statement: SELECT * FROM customer_profiles WHERE secret_key = 'key_vdt_2026_%s' AND status = 'ACTIVE';", "SELECT"},
        {"LOG: statement: UPDATE account_credentials SET api_password = 'pass_vng_fpt_%s' WHERE user_id = %d;", "UPDATE"}, 
        {"LOG: statement: DROP TABLE temporary_debug_logs_%s_v%d;", "DROP"},
        {"LOG: statement: ALTER SYSTEM SET max_connections = %2$d; -- profile_%1$s", "ALTER"}
    };

    /**
     * Tự động bắn dữ liệu giả lập đa dạng và ngẫu nhiên vào Kafka sau mỗi 5 giây (để data nhiều hơn tí)
     */
    @Scheduled(fixedRate = 5000)
    public void simulateLogs() {
        // 1. Tạo và bắn Log ngẫu nhiên cho MySQL
        simulateMysqlLog();

        // 2. Tạo và bắn Log ngẫu nhiên cho Postgres
        simulatePostgresLog();
    }

    private void simulateMysqlLog() {
        String table = MYSQL_TABLES[random.nextInt(MYSQL_TABLES.length)];
        int queryIdx = random.nextInt(MYSQL_QUERIES.length);
        
        String action = MYSQL_QUERIES[queryIdx][1];
        String rawQueryTemplate = MYSQL_QUERIES[queryIdx][0];
        
        // Sinh số ngẫu nhiên để điền vào template query
        int val1 = random.nextInt(10000);
        int val2 = random.nextInt(1000);
        int val3 = random.nextInt(100);
        String formattedQuery = String.format(rawQueryTemplate, val1, val2, val3);

        // Escape ký tự dấu nháy kép cho JSON hợp lệ
        String safeQuery = formattedQuery.replace("\"", "\\\"");

        String mockMysqlMessage = String.format(
            "{\"table\":\"%s\", \"action\":\"%s\", \"query\":\"%s\"}", 
            table, action, safeQuery
        );
        
        String mockMysqlKey = "mysql-prod-db-" + (random.nextInt(2) + 1);

        log.info("[Simulator] Đang bắn 1 log mẫu ngẫu nhiên của MySQL vào Kafka (Action: {})...", action);
        kafkaTemplate.send("mysql-raw-logs", mockMysqlKey, mockMysqlMessage);
    }

    private void simulatePostgresLog() {
        int statementIdx = random.nextInt(POSTGRES_STATEMENTS.length);
        String action = POSTGRES_STATEMENTS[statementIdx][1];
        String rawStatementTemplate = POSTGRES_STATEMENTS[statementIdx][0];

        // Sinh chuỗi/số ngẫu nhiên để nhét vào token, mật khẩu
        String randString = UUID.randomUUID().toString().substring(0, 8);
        int randNum1 = random.nextInt(5000);
        int randNum2 = random.nextInt(200);
        String mockPostgresMessage = String.format(rawStatementTemplate, randString, randNum1, randNum2);

        String mockPostgresKey = "postgres-core-db-" + (random.nextInt(2) + 1);

        log.info("[Simulator] Đang bắn 1 log mẫu ngẫu nhiên của Postgres vào Kafka (Action: {})...", action);
        kafkaTemplate.send("postgres-raw-logs", mockPostgresKey, mockPostgresMessage);
    }
}