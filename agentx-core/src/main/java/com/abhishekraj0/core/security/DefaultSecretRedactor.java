package com.abhishekraj0.core.security;

import com.abhishekraj0.api.security.SecretRedactor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Default implementation of SecretRedactor using predefined and custom regular expression patterns.
 */
public class DefaultSecretRedactor implements SecretRedactor {

    public static final class RedactionRule {
        private final Pattern pattern;
        private final String replacement;

        public RedactionRule(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public RedactionRule(String regex, String replacement) {
            this.pattern = Pattern.compile(regex);
            this.replacement = replacement;
        }

        public String apply(String text) {
            return pattern.matcher(text).replaceAll(replacement);
        }
    }

    private final List<RedactionRule> rules = new CopyOnWriteArrayList<>();

    public DefaultSecretRedactor() {
        // Register default rules
        // 1. OpenAI and standard API key patterns: sk-proj-...
        rules.add(new RedactionRule("(?i)\\b(sk-[a-zA-Z0-9\\-]+)\\b", "[REDACTED]"));

        // 2. Authorization headers (Bearer, Basic token values)
        rules.add(new RedactionRule("(?i)(Authorization\\s*:\\s*(Bearer|Basic)\\s+)[a-zA-Z0-9_\\-\\.\\~\\+\\/\\=]+", "$1[REDACTED]"));

        // 3. Password assignments: password: xyz, password = abc
        rules.add(new RedactionRule("(?i)\\b(password|passwd|passcode)(\\s*(:|=)\\s*)[a-zA-Z0-9_\\-\\@\\#\\$\\%\\^\\&\\*\\(\\)\\[\\]\\{\\}\\!\\?\\+\\=]{4,}", "$1$2[REDACTED]"));

        // 4. API Key/Secret Key/Auth Token assignments
        rules.add(new RedactionRule("(?i)\\b(api[-_]?key|secret[-_]?key|auth[-_]?token|private[-_]?key)(\\s*(:|=)\\s*)[a-zA-Z0-9_\\-\\.\\~\\+\\/\\=\\!]{6,}", "$1$2[REDACTED]"));

        // 5. Connection strings credentials (jdbc:postgresql://user:pass@host...)
        rules.add(new RedactionRule("(?i)(jdbc:[a-zA-Z0-9\\+\\.\\-]+://[^:]+:)([^@\\s]+)(@[^\\s\\?]+)", "$1[REDACTED]$3"));
    }

    public void addRule(RedactionRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    public void addRule(String regex, String replacement) {
        if (regex != null && replacement != null) {
            rules.add(new RedactionRule(regex, replacement));
        }
    }

    @Override
    public String redact(String text) {
        if (text == null) {
            return null;
        }

        String result = text;
        for (RedactionRule rule : rules) {
            result = rule.apply(result);
        }
        return result;
    }
}
