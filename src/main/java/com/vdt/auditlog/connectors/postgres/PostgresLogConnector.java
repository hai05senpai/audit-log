package com.vdt.auditlog.connectors.postgres;

import com.vdt.auditlog.connectors.base.LogConnector;
import com.vdt.auditlog.kafka.producer.RawLogProducer; // ◄ Import Producer
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
    private final RawLogProducer rawLogProducer;
    
    private Connection connection;
    private PGReplicationStream stream;
    private boolean running = false;

    // ◄ Thiết kế lại Constructor để khớp chính xác với ConnectorManager
    public PostgresLogConnector(String host, int port, String dbName, String username, String password, 
                                String slotName, String connectorName, RawLogProducer rawLogProducer) {
        this.url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        this.connectorName = connectorName;
        this.slotName = slotName;
        this.rawLogProducer = rawLogProducer;

        this.props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("assumeMinServerVersion", "9.4");
        props.setProperty("replication", "database");
    }

    @Override
    public void start() throws Exception {
        log.info("Đang khởi động Postgres Connector: {}", connectorName);
        this.connection = DriverManager.getConnection(url, props);
        PGConnection pgConnection = connection.unwrap(PGConnection.class);

        this.stream = pgConnection.getReplicationAPI()
                .replicationStream()
                .logical()
                .withSlotName(slotName)
                .withSlotOption("include-xids", "true")
                .withSlotOption("skip-empty-xacts", "true")
                .start();

        this.running = true;
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

                int offset = msg.arrayOffset();
                byte[] source = msg.array();
                String rawLogPayload = new String(source, offset, msg.remaining());

                log.info("[{}] Phát hiện sự kiện WAL từ Postgres: {}", connectorName, rawLogPayload);

                try {
                    // ◄ Gọi đúng hàm sendRawLog của RawLogProducer
                    rawLogProducer.sendRawLog("postgres-raw-logs", connectorName, rawLogPayload);
                } catch (Exception e) {
                    log.error("[{}] Lỗi khi gửi dữ liệu sang Kafka Producer", connectorName, e);
                }

                // Cập nhật vị trí LSN làm Checkpoint
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