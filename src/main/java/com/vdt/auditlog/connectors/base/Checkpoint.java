package com.vdt.auditlog.connectors.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {
    private String connectorName;
    private String logFileName; // Tên file binlog (MySQL) hoặc WAL segment (Postgres)
    private long position;      // Vị trí offset (Binlog position hoặc Postgres LSN)
    private long lastTimestamp; // Thời gian cập nhật checkpoint gần nhất
}