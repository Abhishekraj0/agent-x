package com.abhishekraj0.api.agent;

/**
 * Token propagated through layers to signal cancellation requests.
 */
public interface CancellationToken {

    boolean isCancelled();

    void throwIfCancelled() throws RuntimeException;

    void onCancel(Runnable callback);
}
