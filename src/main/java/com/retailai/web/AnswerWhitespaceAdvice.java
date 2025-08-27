package com.retailai.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Repairs whitespace in API responses to avoid "jammed" text like
 * "I'mnotbeingaskedanyspecificquestions..."
 *
 * It looks for keys named "answer" or "reply" (case-insensitive) in Map bodies,
 * and for long strings with zero spaces. Then applies a heuristic "smartSpace" fixer.
 */
@ControllerAdvice
public class AnswerWhitespaceAdvice implements ResponseBodyAdvice<Object> {

    private static final Set<String> TARGET_KEYS = Set.of("answer","reply");
    private static final Pattern HAS_WS = Pattern.compile("\\s+");

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true; // inspect all responses
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof Map<?,?> map) {
            Map<Object,Object> copy = new LinkedHashMap<>(map.size());
            map.forEach((k, v) -> {
                if (k instanceof String key && shouldFixKey(key) && v instanceof String s) {
                    copy.put(k, fixText(s));
                } else if (v instanceof Map<?,?> nested) {
                    copy.put(k, fixMapRecursively((Map<?,?>) nested));
                } else if (v instanceof List<?> list) {
                    copy.put(k, fixListRecursively(list));
                } else {
                    copy.put(k, v);
                }
            });
            return copy;
        }

        if (body instanceof List<?> list) {
            return fixListRecursively(list);
        }

        // not a map/list — leave as is
        return body;
    }

    private static boolean shouldFixKey(String key) {
        return TARGET_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    private static Map<Object,Object> fixMapRecursively(Map<?,?> in) {
        Map<Object,Object> out = new LinkedHashMap<>(in.size());
        in.forEach((k, v) -> {
            if (k instanceof String key && shouldFixKey(key) && v instanceof String s) {
                out.put(k, fixText(s));
            } else if (v instanceof Map<?,?> m) {
                out.put(k, fixMapRecursively(m));
            } else if (v instanceof List<?> list) {
                out.put(k, fixListRecursively(list));
            } else {
                out.put(k, v);
            }
        });
        return out;
    }

    private static List<Object> fixListRecursively(List<?> in) {
        List<Object> out = new ArrayList<>(in.size());
        for (Object v : in) {
            if (v instanceof Map<?,?> m) out.add(fixMapRecursively(m));
            else if (v instanceof List<?> l) out.add(fixListRecursively(l));
            else out.add(v);
        }
        return out;
    }

    /** Main fixer: normalize exotic spaces, repair when no spaces, keep newlines. */
    private static String fixText(String s) {
        if (s == null) return null;
        final String original = s;

        // Normalize newlines and exotic spaces → regular space
        s = s.replace("\r\n", "\n")
             .replace("\u00A0", " ")
             .replace("\u2007", " ")
             .replace("\u202F", " ");

        if (!HAS_WS.matcher(s).find()) {
            // No whitespace at all → reinsert heuristically

            // Add space after sentence punctuation . , ; : ! ?
            s = s.replaceAll("([.,;:!?])(?!\\s)", "$1 ");

            // Split camel bumps / letter-case boundaries
            s = s.replaceAll("(?<=[a-z])(?=[A-Z])", " ");

            // Split letter↔digit boundaries
            s = s.replaceAll("(?<=[A-Za-z])(?=\\d)", " ");
            s = s.replaceAll("(?<=\\d)(?=[A-Za-z])", " ");

            // Add space after bullets * or - if missing
            s = s.replaceAll("([*-])(?!\\s)", "$1 ");

            // Add space after apostrophe if next char is a letter (fixes "I'mnot" → "I'm not")
            s = s.replaceAll("(?<=[A-Za-z][’'])(?=[A-Za-z])", " ");
        }

        // Collapse multiple spaces (keep newlines), trim
        s = s.replaceAll("[ \\t]{2,}", " ")
             .replaceAll("\\n{3,}", "\n\n")
             .trim();

        // If the fix made it worse somehow, fall back
        return (s.isEmpty() ? original : s);
    }
}
