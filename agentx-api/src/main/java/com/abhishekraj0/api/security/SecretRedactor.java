package com.abhishekraj0.api.security;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Interface representing a component that redacts sensitive information (secrets) from logs, history, and outputs.
 */
public interface SecretRedactor {

    /**
     * Redacts secrets in the input text.
     *
     * @param text the text to inspect and redact
     * @return the redacted text
     */
    String redact(String text);

    AtomicReference<SecretRedactor> instance = new AtomicReference<>();

    /**
     * Gets the globally configured SecretRedactor instance.
     *
     * @return the redactor instance
     */
    static SecretRedactor getInstance() {
        SecretRedactor current = instance.get();
        if (current == null) {
            try {
                Class<?> clazz = Class.forName("com.abhishekraj0.core.security.DefaultSecretRedactor");
                current = (SecretRedactor) clazz.getDeclaredConstructor().newInstance();
                instance.compareAndSet(null, current);
            } catch (Exception e) {
                // Fallback to no-op if core classes aren't present
                current = text -> text;
                instance.compareAndSet(null, current);
            }
        }
        return instance.get();
    }

    /**
     * Sets the globally configured SecretRedactor instance.
     *
     * @param redactor the redactor to set
     */
    static void setInstance(SecretRedactor redactor) {
        instance.set(redactor);
    }
}
