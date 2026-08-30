package com.abhishekraj0.core.agent;

import com.abhishekraj0.api.agent.CancellationToken;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of CancellationToken supporting callbacks, throwing on cancel,
 * and a global thread-safe registry to lookup tokens by executionId.
 */
public class DefaultCancellationToken implements CancellationToken, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Map<String, CancellationToken> registry = new ConcurrentHashMap<>();

    public static CancellationToken get(String executionId) {
        if (executionId == null) {
            return null;
        }
        return registry.get(executionId);
    }

    public static void register(String executionId, CancellationToken token) {
        if (executionId != null && token != null) {
            registry.put(executionId, token);
        }
    }

    public static void deregister(String executionId) {
        if (executionId != null) {
            registry.remove(executionId);
        }
    }

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final transient List<Runnable> callbacks = new CopyOnWriteArrayList<>();

    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            if (callbacks != null) {
                for (Runnable callback : callbacks) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        // ignore callback exception to make sure all run
                    }
                }
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new CancellationException("Execution was cancelled");
        }
    }

    @Override
    public void onCancel(Runnable callback) {
        if (callback != null) {
            callbacks.add(callback);
            if (isCancelled()) {
                try {
                    callback.run();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
