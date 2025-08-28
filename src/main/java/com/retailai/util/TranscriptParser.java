package com.retailai.util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class TranscriptParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static String parseAndFormatTranscript(String raw) {
        if (raw == null || raw.isBlank()) return "—";

        try {
            String cleaned = raw.trim();

            // 🔹 If it starts with a quote, it's a JSON string → unwrap it
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = MAPPER.readValue(cleaned, String.class);
            }

            // 🔹 Now parse the actual JSON array
            List<Map<String, String>> turns = MAPPER.readValue(
                    cleaned, new TypeReference<List<Map<String, String>>>() {}
            );

            // 🔹 Build conversation text
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> turn : turns) {
                String type = turn.get("type");
                String text = turn.get("text");
                String who = ("user".equalsIgnoreCase(type)) ? "User" : "Assistant";
                sb.append(who).append(": ").append(text).append("\n");
            }
            return sb.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return raw; // fallback: return raw if parse fails
        }
    }
    public static String formatTranscriptMore(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            // If it's wrapped as a JSON string, unwrap first
            String cleaned = raw.trim();
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = MAPPER.readValue(cleaned, String.class);
            }

            // Parse JSON array into list of maps
            List<Map<String, String>> turns = MAPPER.readValue(
                    cleaned, new TypeReference<List<Map<String, String>>>() {}
            );

            // Build plain text
            StringBuilder sb = new StringBuilder();
            for (Map<String, String> turn : turns) {
                String type = turn.get("type");
                String text = turn.get("text");
                String who = ("user".equalsIgnoreCase(type)) ? "User" : "Assistant";
                sb.append(who).append(": ").append(text).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return raw; // fallback
        }
    }
    /** Convert transcript JSON (array or double-escaped array) into plain "User:/Assistant:" lines. */
    public static String formatTranscript(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        try {
            String arrayJson = normalizeToArrayJson(raw);
            List<Map<String, Object>> turns = MAPPER.readValue(
                    arrayJson, new TypeReference<List<Map<String, Object>>>() {}
            );

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> turn : turns) {
                String type = String.valueOf(turn.get("type"));
                String text = String.valueOf(turn.get("text"));
                String who = ("user".equalsIgnoreCase(type) || "customer".equalsIgnoreCase(type))
                        ? "User" : "Assistant";
                sb.append(who).append(": ").append(text).append('\n');
            }

            return sb.toString().trim();
        } catch (Exception e) {
            // Fallback: return original so you can see what came in
            return raw;
        }
    }

    /** Ensure we have a clean JSON array string like: [{"type":"user","text":"Hi"}, ...] */
    private static String normalizeToArrayJson(String raw) throws JsonProcessingException {
        String s = raw.trim();

        // Case 1: it already begins with '[' => it's a JSON array -> return as-is
        if (s.startsWith("[")) {
            return s;
        }

        // Case 2: it is a JSON-encoded string that itself contains the array
        // Try up to 3 unwrapping passes in case of multiple escaping layers
        for (int i = 0; i < 3; i++) {
            // Attempt to decode as a JSON string
            try {
                s = MAPPER.readValue(s, String.class); // unwrap one string layer
                String t = s.trim();
                if (t.startsWith("[")) {
                    return t; // now it's an array json
                }
                s = t; // continue loop if still not an array
                continue;
            } catch (JsonProcessingException ignored) {
                // Not a clean JSON string; try manual quote trim/unescape once
                String t = stripOuterQuotes(s);
                if (!t.equals(s)) {
                    s = t.replace("\\\"", "\"").replace("\\\\", "\\").trim();
                    if (s.startsWith("[")) return s;
                    // loop again in case another layer exists
                    continue;
                }
                // Can't unwrap further
                break;
            }
        }

        // As a last resort: if it *looks* like an array but with \" inside,
        // use the "wrap then decode" trick.
        if (s.contains("\\\"")) {
            String jsonStringLiteral = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            String unescaped = MAPPER.readValue(jsonStringLiteral, String.class);
            if (unescaped.trim().startsWith("[")) return unescaped.trim();
        }

        // Give back whatever we have (caller will likely fall back to raw output)
        return s;
    }

    private static String stripOuterQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    // --- Quick test ---
    public static void main(String[] args) {
        // Already-an-array BUT with backslash-escaped quotes:
        String input1 = "\"[{\\\"type\\\":\\\"user\\\",\\\"text\\\":\\\" Hello?\\\"},{\\\"type\\\":\\\"gpt\\\",\\\"text\\\":\\\"Hello my name is Manisha from Radhe collections\\\"}]\"";
       // String input2 ="\"[{\\\"type\\\":\\\"user\\\",\\\"text\\\":\\\" Hello?\\\"},{\\\"type\\\":\\\"gpt\\\",\\\"text\\\":\\\"Hello my name is Manisha from Radhe collections\\\"},{\\\"type\\\":\\\"user\\\",\\\"text\\\":\\\" Yes.\\\"},{\\\"type\\\":\\\"gpt\\\",\\\"text\\\":\\\"Hello Jayesh Lodhiya, my name is Jennifer, and I’m your real estate inquiry specialist. Would you like to hear about how we can assist you in selling your property?\\\"}]\"";
        // Double-escaped variant (wrapped in quotes):
     //   String input2 = "\"[{\\\"type\\\":\\\"user\\\",\\\"text\\\":\\\" Hello?\\\"},{\\\"type\\\":\\\"gpt\\\",\\\"text\\\":\\\"Hello my name is Manisha from Radhe collections\\\"}]\"";
        String input2 = "\"[{\\\"type\\\":\\\"user\\\",\\\"text\\\":\\\" Hello?\\\"},{\\\"type\\\":\\\"gpt\\\",\\\"text\\\":\\\"Hello my name is Manisha ...}]\"";
        System.out.println("---- input1 ----");
        System.out.println(wrapAsJsonString(formatTranscript(input1)));
        System.out.println("\n---- input2 ----");
        System.out.println(formatTranscript(input2));
    }
    public static String wrapAsJsonString(String raw) {
        if (raw == null) return null;
        // Escape backslashes and quotes for JSON string
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        // Surround with quotes
        return "\"" + escaped + "\"";
    }
}
