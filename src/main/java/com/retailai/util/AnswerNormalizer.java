package com.retailai.util;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.text.BreakIterator;
import java.util.Locale;

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

    private static String reinsertSpacesWithBreakIterator(String input, Locale locale) {
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
}
