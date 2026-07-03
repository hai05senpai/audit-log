package com.vdt.auditlog.normalization.masking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataMaskingServiceTest {

    private DataMaskingService dataMaskingService;

    @BeforeEach
    void setUp() {
        dataMaskingService = new DataMaskingService();
    }

    @Test
    void testMaskSensitiveData_WithPassword_ShouldMask() {
        String rawQuery = "UPDATE users SET password = 'plain_pass_123' WHERE username = 'haibui';";
        String result = dataMaskingService.maskSensitiveData(rawQuery);
        
        assertFalse(result.contains("plain_pass_123"), "Mật khẩu thô không được xuất hiện!");
        assertTrue(result.contains("******"), "Phải chứa chuỗi làm mờ ******");
    }

    @Test
    void testMaskSensitiveData_WithJsonToken_ShouldMask() {
        String rawJson = "{\"token\": \"jwt_live_tok_999\", \"user\": 1}";
        String result = dataMaskingService.maskSensitiveData(rawJson);
        
        assertFalse(result.contains("jwt_live_tok_999"), "Token thô không được xuất hiện!");
        assertTrue(result.contains("******"), "Phải chứa chuỗi làm mờ ******");
    }

    @Test
    void testMaskSensitiveData_NoSensitiveData_ShouldReturnOriginal() {
        String normalQuery = "SELECT * FROM products WHERE id = 10;";
        String result = dataMaskingService.maskSensitiveData(normalQuery);
        
        assertEquals(normalQuery, result, "Nếu không có dữ liệu nhạy cảm, chuỗi phải giữ nguyên gốc.");
    }
}