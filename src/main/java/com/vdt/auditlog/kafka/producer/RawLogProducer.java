package com.vdt.auditlog.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RawLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Gửi log thô vào Kafka Topic chỉ định
     * @param topic Tên topic xử lý ("mysql-raw-logs" hoặc "postgres-raw-logs")
     * @param key Khóa định danh (dùng tên connector hoặc database_id để đảm bảo log cùng nguồn luôn vào 1 partition)
     * @param payload Chuỗi dữ liệu log thô (JSON hoặc String text)
     */
    public void sendRawLog(String topic, String key, String payload) {
        log.debug("Đang đẩy log lên Kafka -> Topic: {}, Key: {}", topic, key);

        // Gửi bất đồng bộ để tránh block luồng đọc của Connector
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, payload);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Đẩy log thành công lên Kafka: Offset [{}]", result.getRecordMetadata().offset());
            } else {
                // Yêu cầu phi chức năng số 2: Log lỗi để phục vụ việc giám sát và retry
                log.error("Thất bại khi đẩy log lên Kafka dữ liệu thuộc Key: {}. Lỗi: {}", key, ex.getMessage());
            }
        });
    }
}