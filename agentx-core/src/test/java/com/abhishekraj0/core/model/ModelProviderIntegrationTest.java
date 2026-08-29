package com.abhishekraj0.core.model;

import static org.junit.jupiter.api.Assertions.*;

import com.abhishekraj0.api.model.*;
import com.abhishekraj0.core.model.ollama.OllamaChatModel;
import com.abhishekraj0.core.model.openai.OpenAIChatModel;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ModelProviderIntegrationTest {

    private static HttpServer server;
    private static int port;

    @BeforeAll
    public static void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        
        server.createContext("/chat/completions", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "{\n" +
                        "  \"choices\": [\n" +
                        "    {\n" +
                        "      \"message\": {\n" +
                        "        \"role\": \"assistant\",\n" +
                        "        \"content\": \"Hello from OpenAI!\"\n" +
                        "      },\n" +
                        "      \"finish_reason\": \"stop\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"usage\": {\n" +
                        "    \"prompt_tokens\": 10,\n" +
                        "    \"completion_tokens\": 5,\n" +
                        "    \"total_tokens\": 15\n" +
                        "  }\n" +
                        "}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.createContext("/api/chat", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "{\n" +
                        "  \"message\": {\n" +
                        "    \"role\": \"assistant\",\n" +
                        "    \"content\": \"Hello from Ollama!\"\n" +
                        "  }\n" +
                        "}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.start();
    }

    @AfterAll
    public static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testOpenAIChatModel() {
        OpenAIChatModel model = new OpenAIChatModel("test-key", "gpt-4", "http://localhost:" + port);
        ChatRequest request = new ChatRequest(List.of(ChatMessage.user("Hi")), List.of(), null, null);
        ChatResponse response = model.chat(request);

        assertEquals("Hello from OpenAI!", response.message().content());
        assertEquals(15, response.usage().totalTokens());
        assertEquals("stop", response.finishReason());
    }

    @Test
    public void testOllamaChatModel() {
        OllamaChatModel model = new OllamaChatModel("llama3", "http://localhost:" + port);
        ChatRequest request = new ChatRequest(List.of(ChatMessage.user("Hi")), List.of(), null, null);
        ChatResponse response = model.chat(request);

        assertEquals("Hello from Ollama!", response.message().content());
        assertEquals("stop", response.finishReason());
    }
}
