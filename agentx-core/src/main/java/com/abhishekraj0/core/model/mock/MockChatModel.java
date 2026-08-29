package com.abhishekraj0.core.model.mock;

import com.abhishekraj0.api.model.*;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A mock adapter implementation of ChatModel and StreamingChatModel for testing purposes.
 */
public class MockChatModel implements StreamingChatModel {

    private final ModelMetadata metadata;
    private Function<ChatRequest, ChatResponse> handler;

    /**
     * Creates a MockChatModel with custom model properties.
     *
     * @param id       the model ID
     * @param provider the provider name
     */
    public MockChatModel(String id, String provider) {
        this.metadata = new ModelMetadata(id, provider, 8192, true, true);
        this.handler = request -> {
            String lastUserMessage = "";
            if (request.messages() != null && !request.messages().isEmpty()) {
                lastUserMessage = request.messages().get(request.messages().size() - 1).content();
            }
            return new ChatResponse(
                    ChatMessage.assistant("Mock response to: " + lastUserMessage),
                    new TokenUsage(10, 10, 20),
                    "STOP"
            );
        };
    }

    /**
     * Creates a MockChatModel with default properties.
     */
    public MockChatModel() {
        this("mock-model", "mock-provider");
    }

    /**
     * Registers a custom handler function to generate chat responses dynamically.
     *
     * @param handler the handler function
     */
    public void setHandler(Function<ChatRequest, ChatResponse> handler) {
        this.handler = handler;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return handler.apply(request);
    }

    @Override
    public ModelMetadata metadata() {
        return metadata;
    }

    @Override
    public Publisher<ChatChunk> stream(ChatRequest request) {
        ChatResponse response = chat(request);
        String content = response.message().content();
        String[] chunks = content.split("(?<=\\s)"); // split keeping spaces
        return new Publisher<>() {
            @Override
            public void subscribe(Subscriber<? super ChatChunk> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    private int index = 0;
                    private boolean cancelled = false;

                    @Override
                    public void request(long n) {
                        if (cancelled) return;
                        for (int i = 0; i < n; i++) {
                            if (index < chunks.length) {
                                String chunkText = chunks[index++];
                                boolean last = (index == chunks.length);
                                subscriber.onNext(new ChatChunk(chunkText, null, last));
                                if (last) {
                                    subscriber.onComplete();
                                    break;
                                }
                            } else {
                                subscriber.onComplete();
                                break;
                            }
                        }
                    }

                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                });
            }
        };
    }
}
