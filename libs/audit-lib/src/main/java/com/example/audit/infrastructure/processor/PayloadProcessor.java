package com.example.audit.infrastructure.processor;

import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.processor.maskers.CreditCardFieldMasker;
import com.example.audit.infrastructure.processor.maskers.EmailFieldMasker;
import com.example.audit.infrastructure.processor.maskers.PasswordFieldMasker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Processes method arguments into audit payload JSON.
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>JSON serialization of arguments</li>
 *   <li>Field masking for sensitive data (using type-aware maskers)</li>
 *   <li>Payload truncation when exceeding size limit (64KB default)</li>
 *   <li>Circular reference detection</li>
 * </ul>
 */
public class PayloadProcessor {

    private static final Logger log = LoggerFactory.getLogger(PayloadProcessor.class);
    private static final String DEFAULT_MASK_VALUE = "********";
    private static final int MAX_DEPTH = 10;

    private final ObjectMapper objectMapper;
    private final AuditProperties auditProperties;
    private final List<FieldMasker> maskers;

    public PayloadProcessor(ObjectMapper objectMapper, AuditProperties auditProperties) {
        this(objectMapper, auditProperties, createDefaultMaskers());
    }

    public PayloadProcessor(ObjectMapper objectMapper, AuditProperties auditProperties, List<FieldMasker> maskers) {
        this.objectMapper = objectMapper.copy();
        // Configure to handle circular references
        this.objectMapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        this.objectMapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        this.auditProperties = auditProperties;
        // Sort maskers by priority (highest first)
        this.maskers = new ArrayList<>(maskers);
        this.maskers.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    private static List<FieldMasker> createDefaultMaskers() {
        return List.of(
                new PasswordFieldMasker(),
                new CreditCardFieldMasker(),
                new EmailFieldMasker()
        );
    }

    /**
     * Process method arguments into a JSON payload.
     *
     * @param args       the method arguments
     * @param maskFields fields to mask (supports dot notation for nested)
     * @return processed payload with truncation flag
     */
    public ProcessedPayload process(Object[] args, String[] maskFields) {
        try {
            // Convert arguments to serializable structure
            Object serializable = convertToSerializable(args, 0);

            // Apply masking
            Set<String> allMaskFields = new HashSet<>(auditProperties.getMasking().getDefaultFields());
            if (maskFields != null) {
                allMaskFields.addAll(Arrays.asList(maskFields));
            }

            if (serializable instanceof Map) {
                applyMasking((Map<String, Object>) serializable, allMaskFields, "");
            } else if (serializable instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        applyMasking((Map<String, Object>) item, allMaskFields, "");
                    }
                }
            }

            // Serialize to JSON
            String json = objectMapper.writeValueAsString(serializable);

            // Check size limit
            int maxSize = auditProperties.getPayload().getMaxSize();
            if (json.length() > maxSize) {
                return truncatePayload(json, maxSize);
            }

            return new ProcessedPayload(json, false);

        } catch (Exception e) {
            log.warn("Failed to process audit payload", e);
            return new ProcessedPayload("{\"_error\": \"payload serialization failed: " +
                    e.getMessage().replace("\"", "'") + "\"}", false);
        }
    }

    private Object convertToSerializable(Object[] args, int depth) {
        if (depth > MAX_DEPTH) {
            return "[max depth exceeded]";
        }

        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }

        if (args.length == 1) {
            return convertSingleArg(args[0], depth);
        }

        // Multiple arguments - create indexed map
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            result.put("arg" + i, convertSingleArg(args[i], depth));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertSingleArg(Object arg, int depth) {
        if (arg == null) {
            return null;
        }

        if (depth > MAX_DEPTH) {
            return "[max depth exceeded]";
        }

        // Handle primitives and common types
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return arg;
        }

        if (arg instanceof Enum<?>) {
            return ((Enum<?>) arg).name();
        }

        // Handle collections
        if (arg instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>();
            for (Object item : collection) {
                list.add(convertSingleArg(item, depth + 1));
            }
            return list;
        }

        // Handle maps
        if (arg instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(key, convertSingleArg(entry.getValue(), depth + 1));
            }
            return result;
        }

        // Handle arrays
        if (arg.getClass().isArray()) {
            if (arg instanceof Object[] objArray) {
                return convertToSerializable(objArray, depth + 1);
            }
            // Primitive arrays - convert to string representation
            return Arrays.toString((Object[]) new Object[]{arg});
        }

        // Try to convert complex objects via Jackson
        try {
            String json = objectMapper.writeValueAsString(arg);
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            // Fall back to toString
            return arg.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMasking(Map<String, Object> data, Set<String> maskFields, String prefix) {
        if (data == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = entry.getValue();

            // Check if this field should be masked
            if (shouldMask(fullPath, maskFields)) {
                String stringValue = value != null ? String.valueOf(value) : null;
                data.put(key, maskValue(key, stringValue));
            } else if (value instanceof Map) {
                // Recurse into nested objects
                applyMasking((Map<String, Object>) value, maskFields, fullPath);
            } else if (value instanceof List<?> list) {
                // Handle lists of maps
                for (Object item : list) {
                    if (item instanceof Map) {
                        applyMasking((Map<String, Object>) item, maskFields, fullPath);
                    }
                }
            }
        }
    }

    private boolean shouldMask(String fieldPath, Set<String> maskFields) {
        // Exact match
        if (maskFields.contains(fieldPath)) {
            return true;
        }

        // Check just the field name (for default masks like "password")
        String fieldName = fieldPath.contains(".") ?
                fieldPath.substring(fieldPath.lastIndexOf('.') + 1) : fieldPath;
        return maskFields.contains(fieldName);
    }

    private String maskValue(String fieldName, String value) {
        // Find a masker that supports this field
        for (FieldMasker masker : maskers) {
            if (masker.supports(fieldName)) {
                return masker.mask(value);
            }
        }
        // Default masking if no specific masker found
        return DEFAULT_MASK_VALUE;
    }

    private ProcessedPayload truncatePayload(String originalJson, int maxSize) {
        Map<String, Object> truncated = new LinkedHashMap<>();
        truncated.put("_truncated", true);
        truncated.put("_originalSize", originalJson.length());
        truncated.put("_maxSize", maxSize);

        // Try to include a summary if possible
        try {
            truncated.put("_preview", originalJson.substring(0, Math.min(200, originalJson.length())) + "...");
        } catch (Exception e) {
            // Ignore
        }

        try {
            return new ProcessedPayload(objectMapper.writeValueAsString(truncated), true);
        } catch (JsonProcessingException e) {
            return new ProcessedPayload("{\"_truncated\":true,\"_error\":\"serialization failed\"}", true);
        }
    }

    /**
     * Process an already-serialized JSON payload with masking.
     *
     * @param jsonPayload the JSON string to process
     * @param maskFields  fields to mask
     * @return processed payload with truncation flag
     */
    @SuppressWarnings("unchecked")
    public ProcessedPayload processJsonPayload(String jsonPayload, String[] maskFields) {
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            return new ProcessedPayload(jsonPayload, false);
        }

        try {
            // Parse JSON
            Object parsed = objectMapper.readValue(jsonPayload, Object.class);

            // Apply masking
            Set<String> allMaskFields = new HashSet<>(auditProperties.getMasking().getDefaultFields());
            if (maskFields != null) {
                allMaskFields.addAll(Arrays.asList(maskFields));
            }

            if (parsed instanceof Map) {
                applyMasking((Map<String, Object>) parsed, allMaskFields, "");
            } else if (parsed instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        applyMasking((Map<String, Object>) item, allMaskFields, "");
                    }
                }
            }

            // Serialize back to JSON
            String json = objectMapper.writeValueAsString(parsed);

            // Check size limit
            int maxSize = auditProperties.getPayload().getMaxSize();
            if (json.length() > maxSize) {
                return truncatePayload(json, maxSize);
            }

            return new ProcessedPayload(json, false);

        } catch (Exception e) {
            log.warn("Failed to process JSON payload for masking", e);
            // Return original payload if masking fails
            int maxSize = auditProperties.getPayload().getMaxSize();
            if (jsonPayload.length() > maxSize) {
                return truncatePayload(jsonPayload, maxSize);
            }
            return new ProcessedPayload(jsonPayload, false);
        }
    }

    /**
     * Result of payload processing.
     */
    public record ProcessedPayload(String payload, boolean isTruncated) {
    }
}
