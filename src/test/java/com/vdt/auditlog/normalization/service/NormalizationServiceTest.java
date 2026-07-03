package com.vdt.auditlog.normalization.service;

import com.vdt.auditlog.normalization.masking.DataMaskingService;
import com.vdt.auditlog.normalization.model.AuditLogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizationServiceTest {

    @Mock
    private DataMaskingService maskingService;

    @InjectMocks
    private NormalizationService normalizationService;

    @Test
    void testNormalizeMysqlLog_ShouldReturnUnifiedSchema() {
        String rawMessage = "{\"table\":\"users\", \"action\":\"INSERT\", \"query\":\"INSERT INTO users...\"}";
        String sourceKey = "mysql-prod-db-1";
        
        // Mock hành vi của DataMaskingService
        when(maskingService.maskSensitiveData(anyString())).thenReturn("INSERT INTO users VALUES ('haibui', '******')");

        AuditLogEvent event = normalizationService.normalizeMysqlLog(rawMessage, sourceKey);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals("MYSQL", event.getDbType());
        assertEquals(sourceKey, event.getDbSource());
        assertTrue(event.getQueryStatement().contains("******"));
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testNormalizePostgresLog_ShouldReturnUnifiedSchema() {
        String rawMessage = "LOG: statement: UPDATE account_credentials SET api_password = 'pass_vng' WHERE user_id = 1;";
        String sourceKey = "postgres-core-db-1";

        when(maskingService.maskSensitiveData(anyString())).thenReturn("LOG: statement: UPDATE account_credentials SET api_password = '******' WHERE user_id = 1;");

        AuditLogEvent event = normalizationService.normalizePostgresLog(rawMessage, sourceKey);

        assertNotNull(event);
        assertEquals("POSTGRESQL", event.getDbType());
        assertEquals(sourceKey, event.getDbSource());
        assertTrue(event.getQueryStatement().contains("******"));
    }
}