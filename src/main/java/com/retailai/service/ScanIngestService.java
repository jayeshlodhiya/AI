// src/main/java/com/retailai/service/ScanIngestService.java
package com.retailai.service;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ScanIngestService {

  private final OcrService ocr;     // <-- HTTP-based OCR (e.g., OCR.space), no native libs
  private final RAGService rag;

  public ScanIngestService(@Qualifier("ocrSpaceOcrService") OcrService ocr, RAGService rag) {
    this.ocr = ocr;
    this.rag = rag;
  }

  /**
   * Extract text from a PDF/image using the configured OCR provider,
   * chunk it, and index into the RAG store so chat can use it immediately.
   *
   * @param tenant  tenant/workspace id (e.g., "demo")
   * @param docType logical type: invoice | warranty | policy | note | other
   * @param docId   a stable id for this document (usually derived from filename)
   * @param file    uploaded file (PDF or image)
   */
  public Map<String, Object> scanAndIndex(String tenant, String docType, String docId, MultipartFile file) throws Exception {
    // 1) OCR → plain text (HTTP provider; no Tesseract/JNA)
    String text = Optional.ofNullable(ocr.extractText(file))
      .orElse("")
      .replace("\r", "")
      .trim();

    if (text.isBlank()) {
      text = "[OCR] No text detected (empty output).";
    }

    // 2) Chunk for RAG (≈1200 chars per chunk to keep prompts tidy)
    final int MAX_CHARS = 1200;
    int start = 0;
    int chunks = 0;

    while (start < text.length()) {
      int end = Math.min(text.length(), start + MAX_CHARS);
      String chunk = text.substring(start, end);
      rag.index(
        tenant,
        docType,
        docId + "#" + chunks,
        chunk,
        List.of(docType.toLowerCase(), "scan")  // tags
      );
      start = end;
      chunks++;
    }

    // 3) Return a simple receipt
    return Map.of(
      "status", "ok",
      "tenant", tenant,
      "docType", docType,
      "docId", docId,
      "chars", text.length(),
      "data",text,
      "chunks", chunks
    );
  }
    public Map<String, Object> scanAndIndex(String tenant, String docType, String docId, String text) throws Exception {
        // 1) OCR → plain text (HTTP provider; no Tesseract/JNA)




        // 2) Chunk for RAG (≈1200 chars per chunk to keep prompts tidy)
        final int MAX_CHARS = 1200;
        int start = 0;
        int chunks = 0;

        while (start < text.length()) {
            int end = Math.min(text.length(), start + MAX_CHARS);
            String chunk = text.substring(start, end);
            rag.index(
                    tenant,
                    docType,
                    docId + "#" + chunks,
                    chunk,
                    List.of(docType.toLowerCase(), "scan")  // tags
            );
            start = end;
            chunks++;
        }

        // 3) Return a simple receipt
        return Map.of(
                "status", "ok",
                "tenant", tenant,
                "docType", docType,
                "docId", docId,
                "chars", text.length(),
                "data",text,
                "chunks", chunks
        );
    }
  /**
   * Convenience helper: create a safe docId from original filename.
   */
  public static String safeDocId(MultipartFile file) {
    String name = Objects.toString(file.getOriginalFilename(), "scan");
    return name.replaceAll("[^A-Za-z0-9._-]", "_");
  }
}
