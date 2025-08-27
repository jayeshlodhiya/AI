package com.retailai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

@Service
public class PdfAndImageOcrService implements OcrService {
  @Override
  public String extractText(MultipartFile file) throws Exception {
    String name = (file.getOriginalFilename()==null?"":file.getOriginalFilename()).toLowerCase();
    if (name.endsWith(".pdf")) {
      try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
        return new PDFTextStripper().getText(doc);
      }
    }
    // Enable below if you add Tess4J + tesseract for images
    // if (name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".webp")) {
    //   var img = javax.imageio.ImageIO.read(file.getInputStream());
    //   var t = new net.sourceforge.tess4j.Tesseract(); t.setLanguage("eng");
    //   return t.doOCR(img);
    // }
    return "[OCR not enabled for images in this build. Upload a PDF or enable Tess4J/cloud OCR.]";
  }
}
