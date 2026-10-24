package com.omkarmele.inkwell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omkarmele.inkwell.exception.ApiException;
import com.omkarmele.inkwell.security.AuthGrantService;
import org.junit.jupiter.api.Test;

class AuthGrantServiceTest {
    @Test
    void grantCanOnlyBeConsumedOnce() {
        AuthGrantService service = new AuthGrantService();
        String code = service.issue("user-1");

        assertEquals("user-1", service.consume(code));
        assertThrows(ApiException.class, () -> service.consume(code));
    }
}
