package com.abhishekraj0.api.agent;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Token propagated through layers to signal cancellation requests.
 */
public class CancellationToken implements Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
