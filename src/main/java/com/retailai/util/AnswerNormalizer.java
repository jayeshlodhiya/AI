package com.retailai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.api.dto.Turn;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AnswerNormalizer {
    private AnswerNormalizer(){}
    public static PipedReader toPipedReader(String data) throws IOException {
        PipedWriter writer = new PipedWriter();
        PipedReader reader = new PipedReader(writer);

        // write asynchronously so reader won’t block forever
        new Thread(() -> {
            try (writer) {
                writer.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        return reader;
    }
    public static String normalize(String s) {
        if (s == null) return null;
        // normalize newlines + exotic spaces -> regular spaces
        s = s.replace("\r\n", "\n")
             .replace("\u00A0", " ")
             .replace("\u2007", " ")
             .replace("\u202F", " ");

        // If there is no whitespace at all, try to segment into words
        if (!s.chars().anyMatch(Character::isWhitespace)) {
            s = reinsertSpacesWithBreakIterator(s, Locale.ENGLISH);
        }

        // collapse long runs of spaces (keep newlines)
        s = s.replaceAll("[ \\t]{2,}", " ")
             .replaceAll("\\n{3,}", "\n\n")
             .trim();

        return s;
    }

    public static String reinsertSpacesWithBreakIterator(String input, Locale locale) {
        BreakIterator it = BreakIterator.getWordInstance(locale);
        it.setText(input);
        StringBuilder out = new StringBuilder(input.length() + input.length()/6);
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String token = input.substring(start, end);
            if (token.trim().isEmpty()) continue; // skip any weird non-visible chunks
            if (out.length() > 0) out.append(' ');
            out.append(token);
        }
        String s = out.toString();

        // Heuristic touches: space after punctuation, split digit/letter boundaries, bullets
        s = s.replaceAll("([.,;:!?])(?!\\s)", "$1 ");
        s = s.replaceAll("(?<=[A-Za-z])(?=\\d)", " ");
        s = s.replaceAll("(?<=\\d)(?=[A-Za-z])", " ");
        s = s.replaceAll("([*-])(?!\\s)", "$1 ");
        s = s.replaceAll("(?<=[A-Za-z][’'])(?=[A-Za-z])", " "); // "I'mnot" -> "I'm not"
        return s;
    }
    public static Object normalizeTranscript(Object raw, ObjectMapper mapper) {
        if (raw == null) return null;

        // Already a structured value?
        if (raw instanceof List) return raw;
        if (raw instanceof Map)  return raw;

        // If DB stores as byte[] (common in some drivers)
        if (raw instanceof byte[]) {
            raw = new String((byte[]) raw);
        }

        // Try up to 3 parses (handles "double-encoded" JSON strings)
        if (raw instanceof String s) {
            String current = s.trim();
            for (int i = 0; i < 3; i++) {
                // If it looks like JSON, attempt to parse
                if (current.startsWith("[") || current.startsWith("{")) {
                    try {
                        Object parsed = mapper.readValue(current, Object.class);
                        if (parsed instanceof String ps) {
                            // still a string? loop again
                            current = ps.trim();
                            continue;
                        }
                        // If it's an array of turns, return as-is
                        return parsed;
                    } catch (Exception ignore) {
                        break; // stop trying to parse; fall through to regex
                    }
                }
                break;
            }

            // Regex fallback: extract individual {"type":"...","text":"..."} objects from a noisy string
            try {
                var pattern = java.util.regex.Pattern.compile(
                        "\\{[^{}]*\\\"type\\\"\\s*:\\s*\\\"[^\\\"]*\\\"\\s*,\\s*\\\"text\\\"\\s*:\\s*\\\"(?:\\\\\\\"|[^\\\"])*\\\"[^{}]*\\}");
                var m = pattern.matcher(current);
                java.util.List<Map<String, Object>> arr = new java.util.ArrayList<>();
                while (m.find()) {
                    String objStr = m.group();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> obj = mapper.readValue(objStr, Map.class);
                        arr.add(obj);
                    } catch (Exception ignore) { }
                }
                if (!arr.isEmpty()) return arr;
            } catch (Exception ignore) { }

            // Could not parse — return raw plain text
            return s;
        }

        // Unknown type — serialize to String to keep response JSON-safe
        return String.valueOf(raw);
    }
    public static String asString(Object v) { return v == null ? null : String.valueOf(v); }
    public static Integer asInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.valueOf(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    /** Parse transcript into List<Turn> using multi-pass + regex fallback. */
    public static List<Turn> toTurns(Object raw, ObjectMapper mapper) {
        if (raw == null) return null;

        if (raw instanceof List<?> list) {
            // best effort map to Turn
            List<Turn> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o instanceof Map<?,?> m) {
                    out.add(new Turn(asStr(m.get("type")), asStr(m.get("text"))));
                }
            }
            return out.isEmpty() ? null : out;
        }

        if (raw instanceof byte[]) raw = new String((byte[]) raw);

        if (raw instanceof String s) {
            String current = s.trim();
            // try up to 3 parses
            for (int i = 0; i < 3; i++) {
                if (current.startsWith("[") || current.startsWith("{")) {
                    try {
                        // parse as generic list
                        List<Map<String, Object>> arr =
                                mapper.readValue(current, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>() {});
                        if (arr != null) {
                            List<Turn> out = new ArrayList<>(arr.size());
                            for (Map<String, Object> m : arr) {
                                out.add(new Turn(asStr(m.get("type")), asStr(m.get("text"))));
                            }
                            return out;
                        }
                    } catch (Exception e) {
                        // maybe it's a JSON string inside a JSON string
                        try {
                            String inner = mapper.readValue(current, String.class);
                            current = inner.trim();
                            continue;
                        } catch (Exception ignore) { /* fall through */ }
                    }
                }
                break;
            }

            // regex fallback
            try {
                var pattern = java.util.regex.Pattern.compile(
                        "\\{[^{}]*\\\"type\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"\\s*,\\s*\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\\"|[^\\\"])*)\\\"[^{}]*\\}");
                var m = pattern.matcher(current);
                List<Turn> out = new ArrayList<>();
                while (m.find()) {
                    String type = unescapeJson(m.group(1));
                    String text = unescapeJson(m.group(2));
                    out.add(new Turn(type, text));
                }
                if (!out.isEmpty()) return out;
            } catch (Exception ignore) { }
        }

        return null;
    }

    public static String asStr(Object v) { return v == null ? null : String.valueOf(v); }

    public static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}
