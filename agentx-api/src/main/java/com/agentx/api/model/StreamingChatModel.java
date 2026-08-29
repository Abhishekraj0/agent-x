package com.agentx.api.model;

import org.reactivestreams.Publisher;

/**
 * Interface representing a Chat Model that supports streaming responses.
 */
public interface StreamingChatModel extends ChatModel {

    /**
     * Executes a chat prompt, streaming back chunks of the response.
     *
     * @param request the chat request details
     * @return a publisher of response chunks
     */
    Publisher<ChatChunk> stream(ChatRequest request);
}
