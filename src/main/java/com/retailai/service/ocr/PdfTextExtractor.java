// src/main/java/com/retailai/service/ocr/PdfTextExtractor.java
package com.retailai.service.ocr;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
public class PdfTextExtractor {
  public String extract(MultipartFile file) throws Exception {
    try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
      return new PDFTextStripper().getText(doc);
    }
  }
}
