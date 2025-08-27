package com.retailai.service;

import java.io.*;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.function.Consumer;

public final class TextChunker {

    private TextChunker() {}

    /**
     * Streams text from reader and emits chunks up to chunkSize (chars) with overlap (chars).
     * Uses sentence BreakIterator to split on natural boundaries when possible.
     */
    public static void chunkStream(Reader reader,
                                   int chunkSize,
                                   int overlap,
                                   Consumer<String> onChunk) throws IOException {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        if (overlap < 0 || overlap >= chunkSize) throw new IllegalArgumentException("overlap must be 0..chunkSize-1");

        BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
        StringBuilder buf = new StringBuilder(chunkSize + 8192);
        char[] tmp = new char[8192];
        int n;

        while ((n = br.read(tmp)) != -1) {
            buf.append(tmp, 0, n);

            while (buf.length() >= chunkSize) {
                int cut = findCutIndex(buf, chunkSize);
                String out = buf.substring(0, cut);
                onChunk.accept(out);

                // keep overlap tail
                int tailStart = Math.max(0, cut - overlap);
                String tail = buf.substring(tailStart);
                buf.setLength(0);
                buf.append(tail);
            }
        }

        // flush remainder
        if (buf.length() > 0) {
            onChunk.accept(buf.toString());
        }
    }

    // Try to cut at sentence boundary near chunkSize; fall back to whitespace; else hard cut
    private static int findCutIndex(CharSequence buf, int target) {
        int len = buf.length();
        int max = Math.min(len, target + 1024); // allow a little spill to hit boundary

        // Sentence boundary
        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        bi.setText(buf.toString()); // small cost, but only per chunk window
        int best = -1;
        int b = bi.first();
        for (int e = bi.next(); e != BreakIterator.DONE && e <= max; b = e, e = bi.next()) {
            if (e >= target) { best = e; break; }
            best = e;
        }
        if (best >= 1) return best;

        // Whitespace boundary backward search
        for (int i = Math.min(len, target); i > Math.max(0, target - 512); i--) {
            char c = buf.charAt(i - 1);
            if (Character.isWhitespace(c)) return i;
        }

        // Hard cut
        return Math.min(len, target);
    }
}
