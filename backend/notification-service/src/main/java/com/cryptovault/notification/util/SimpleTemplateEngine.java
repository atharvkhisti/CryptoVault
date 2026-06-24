package com.cryptovault.notification.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <h3>SimpleTemplateEngine</h3>
 *
 * <p><b>Why it exists:</b> Resolves and parses HTML templates loaded from resources, executing key-value variable replacements locally.</p>
 * <p><b>Architectural Layer:</b> Utility Layer.</p>
 * <p><b>Design Patterns Used:</b> Template Method / Substitution Strategy.</p>
 * <p><b>Security Concepts Demonstrated:</b> Keeps parsing local without template engine parser execution bugs, preventing remote code execution (RCE) vectors.</p>
 * <p><b>Future AWS Integration Path:</b> Renders email HTML payloads when triggered asynchronously from AWS SQS consumer listeners.</p>
 * <p><b>Enterprise Relevance:</b> Eliminates template compile dependencies, keeping startup latency and memory usage extremely low.</p>
 * <p><b>Interview Talking Points:</b> Uses standard Java I/O streams to read files from classpath resources and programmatically replaces placeholders without thread-blocking overhead.</p>
 */
@Component
public class SimpleTemplateEngine {

    /**
     * Reads the designated template file and replaces "${key}" patterns with mapped values.
     *
     * @param templateName name of the template file (e.g. registration.html)
     * @param variables    map containing substitution values
     * @return populated HTML string
     */
    public String process(String templateName, Map<String, Object> variables) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateName);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            if (variables == null || variables.isEmpty()) {
                return content;
            }

            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                content = content.replace(placeholder, value);
            }
            return content;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read HTML template: " + templateName, e);
        }
    }
}
