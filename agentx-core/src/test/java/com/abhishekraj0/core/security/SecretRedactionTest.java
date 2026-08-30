package com.abhishekraj0.core.security;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.model.ChatMessage;
import com.abhishekraj0.api.security.SecretRedactor;
import org.junit.jupiter.api.Test;

public class SecretRedactionTest {

    @Test
    public void testDefaultRedactorCommonPatterns() {
        SecretRedactor redactor = SecretRedactor.getInstance();
        assertNotNull(redactor);

        // 1. API Keys (OpenAI style)
        String inputApiKey = "My api key is: sk-proj-1234567890abcdef1234567890";
        assertEquals("My api key is: [REDACTED]", redactor.redact(inputApiKey));

        // 2. Authorization Headers
        String inputAuth = "Header Authorization: Bearer abcdef1234567890xyz";
        assertEquals("Header Authorization: Bearer [REDACTED]", redactor.redact(inputAuth));

        // 3. Passwords
        String inputPassword = "Set password: secret_password_123";
        assertEquals("Set password: [REDACTED]", redactor.redact(inputPassword));

        // 4. Connection Strings
        String jdbcUrl = "jdbc:postgresql://postgres:superSecretPassword@localhost:5432/agentx_db";
        assertEquals("jdbc:postgresql://postgres:[REDACTED]@localhost:5432/agentx_db", redactor.redact(jdbcUrl));
    }

    @Test
    public void testNonSecretsNotModified() {
        SecretRedactor redactor = SecretRedactor.getInstance();

        String safeText = "This is a normal message with 123456 numbers and standard words like postgresql.";
        assertEquals(safeText, redactor.redact(safeText));
    }

    @Test
    public void testExtensibility() {
        DefaultSecretRedactor redactor = new DefaultSecretRedactor();
        
        String text = "Contact phone is +1-555-0199";
        // By default, phone is not redacted
        assertEquals(text, redactor.redact(text));

        // Extend with a custom rule for phone number
        redactor.addRule("\\+1-\\d{3}-\\d{4}", "[REDACTED_PHONE]");
        assertEquals("Contact phone is [REDACTED_PHONE]", redactor.redact(text));
    }

    @Test
    public void testChatMessageRedactionIntegration() {
        // Constructing a ChatMessage should automatically trigger the active SecretRedactor
        ChatMessage message = ChatMessage.user("Access granted with password: MyAdminPassword123!");
        assertEquals("Access granted with password: [REDACTED]", message.content());
    }
}
