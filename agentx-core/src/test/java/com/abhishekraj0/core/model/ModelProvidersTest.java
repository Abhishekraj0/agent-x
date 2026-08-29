package com.abhishekraj0.core.model;

import com.abhishekraj0.api.model.ModelMetadata;
import com.abhishekraj0.core.model.anthropic.AnthropicChatModel;
import com.abhishekraj0.core.model.azure.AzureOpenAIChatModel;
import com.abhishekraj0.core.model.gemini.GeminiChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelProvidersTest {

    @Test
    public void testAnthropicModelMetadata() {
        AnthropicChatModel model = new AnthropicChatModel("test-api-key", "claude-3-5-sonnet");
        ModelMetadata metadata = model.metadata();
        assertNotNull(metadata);
        assertEquals("claude-3-5-sonnet", metadata.id());
        assertEquals("anthropic", metadata.provider());
        assertTrue(metadata.supportsToolCalling());
    }

    @Test
    public void testGeminiModelMetadata() {
        GeminiChatModel model = new GeminiChatModel("test-api-key", "gemini-1.5-pro");
        ModelMetadata metadata = model.metadata();
        assertNotNull(metadata);
        assertEquals("gemini-1.5-pro", metadata.id());
        assertEquals("gemini", metadata.provider());
        assertTrue(metadata.supportsToolCalling());
    }

    @Test
    public void testAzureOpenAIModelMetadata() {
        AzureOpenAIChatModel model = new AzureOpenAIChatModel(
                "test-api-key",
                "https://test-endpoint.openai.azure.com",
                "gpt-4-deployment",
                "2024-02-15-preview"
        );
        ModelMetadata metadata = model.metadata();
        assertNotNull(metadata);
        assertEquals("gpt-4-deployment", metadata.id());
        assertEquals("azure", metadata.provider());
        assertTrue(metadata.supportsToolCalling());
    }
}
