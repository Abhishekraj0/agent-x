package com.agentx.core.model.mock;

import com.agentx.api.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static org.junit.jupiter.api.Assertions.*;

public class MockChatModelTest {

    @Test
    public void testSyncChat() {
        MockChatModel model = new MockChatModel();
        ChatRequest request = ChatRequest.of(List.of(ChatMessage.user("Hello World")));
        ChatResponse response = model.chat(request);

        assertNotNull(response);
        assertEquals("Mock response to: Hello World", response.message().content());
        assertEquals("STOP", response.finishReason());
    }

    @Test
    public void testCustomHandler() {
        MockChatModel model = new MockChatModel();
        model.setHandler(req -> new ChatResponse(
                ChatMessage.assistant("Hello custom response"),
                new TokenUsage(5, 5, 10),
                "STOP"
        ));

        ChatRequest request = ChatRequest.of(List.of(ChatMessage.user("Hello")));
        ChatResponse response = model.chat(request);
        assertEquals("Hello custom response", response.message().content());
    }

    @Test
    public void testStreamingChat() throws InterruptedException {
        MockChatModel model = new MockChatModel();
        ChatRequest request = ChatRequest.of(List.of(ChatMessage.user("Hello")));

        List<ChatChunk> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        model.stream(request).subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ChatChunk chatChunk) {
                chunks.add(chatChunk);
            }

            @Override
            public void onError(Throwable throwable) {
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(chunks.isEmpty());
        
        StringBuilder builder = new StringBuilder();
        for (ChatChunk chunk : chunks) {
            builder.append(chunk.content());
        }
        assertEquals("Mock response to: Hello", builder.toString());
    }
}
