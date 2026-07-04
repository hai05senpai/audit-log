package com.vdt.auditlog.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Topic chứa log thô thu thập từ MySQL
    @Bean
    public NewTopic mysqlRawLogsTopic() {
        return TopicBuilder.name("mysql-raw-logs")
                .partitions(3) // Chia partition để có thể scale-out nhiều consumer chạy song song
                .replicas(1)   // Thay đổi tùy theo cụm Kafka thực tế
                .build();
    }

    // Topic chứa log thô thu thập từ PostgreSQL
    @Bean
    public NewTopic postgresRawLogsTopic() {
        return TopicBuilder.name("postgres-raw-logs")
                .partitions(3)
                .replicas(1)
                .build();
    }
}