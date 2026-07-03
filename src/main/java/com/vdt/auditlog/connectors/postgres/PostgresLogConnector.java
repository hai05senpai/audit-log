package com.vdt.auditlog.connectors.postgres;

import com.vdt.auditlog.connectors.base.LogConnector;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.replication.PGReplicationStream;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PostgresLogConnector implements LogConnector {

    private final String url;
    private final Properties props;
    private final String slotName;
    private final String connectorName;
    
    private Connection connection;
    private PGReplicationStream stream;
    private boolean running = false;

    public PostgresLogConnector(String host, int port, String dbName, String username, String password, String slotName, String connectorName) {
        this.url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        this.connectorName = connectorName;
        this.slotName = slotName;

        this.props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        // Bắt buộc cấu hình thuộc tính này để kích hoạt tính năng Replication trên Connection
        props.setProperty("assumeMinServerVersion", "9.4");
        props.setProperty("replication", "database");
    }

    @Override
    public void start() throws Exception {
        log.info("Đang khởi động Postgres Connector: {}", connectorName);
        this.connection = DriverManager.getConnection(url, props);
        PGConnection pgConnection = connection.unwrap(PGConnection.class);

        // Khởi tạo luồng stream từ Replication Slot đã tạo sẵn trên Postgres
        this.stream = pgConnection.getReplicationAPI()
                .replicationStream()
                .logical()
                .withSlotName(slotName)
                .withSlotOption("include-xids", "true")
                .withSlotOption("skip-empty-xacts", "true")
                .start();

        this.running = true;

        // Tạo vòng lặp nhận log liên tục trên thread độc lập
        new Thread(this::streamLogLoop).start();
    }

    private void streamLogLoop() {
        while (running) {
            try {
                ByteBuffer msg = stream.readPending();
                if (msg == null) {
                    TimeUnit.MILLISECONDS.sleep(100);
                    continue;
                }

                // Chuyển đổi ByteBuffer nhận được thành chuỗi text thông tin thay đổi (Raw WAL log)
                int offset = msg.arrayOffset();
                byte[] source = msg.array();
                String rawLogPayload = new String(source, offset, msg.remaining());

                log.info("[{}] Phát hiện sự kiện WAL từ Postgres: {}", connectorName, rawLogPayload);

                // TODO: Gửi dữ liệu rawLogPayload này vào Kafka Producer tại đây

                // Cập nhật vị trí LSN (Log Sequence Number) đã xử lý thành công để làm Checkpoint
                stream.setAppliedLSN(stream.getLastReceiveLSN());
                stream.setFlushedLSN(stream.getLastReceiveLSN());

            } catch (Exception e) {
                log.error("Lỗi xảy ra trong luồng đọc WAL của Postgres: {}", connectorName, e);
                running = false;
                break;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Đang dừng Postgres Connector: {}", connectorName);
        running = false;
        if (stream != null) stream.close();
        if (connection != null) connection.close();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public String getConnectorName() {
        return this.connectorName;
    }
}