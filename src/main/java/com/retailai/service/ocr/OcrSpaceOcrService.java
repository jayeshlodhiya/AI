package com.retailai.service.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.service.OcrService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Iterator;

@Service
public class OcrSpaceOcrService implements OcrService {

  private static final int OCRSPACE_LIMIT_BYTES = 1024 * 1024; // 1 MB
  private static final float PDF_RENDER_DPI = 140f;            // lower DPI to keep under 1MB/page
  private static final int IMG_MAX_WIDTH = 1600;               // downscale large images
  private static final int IMG_MAX_HEIGHT = 1600;
  private static final float JPEG_START_QUALITY = 0.72f;       // initial JPEG quality
  private static final float JPEG_MIN_QUALITY = 0.40f;         // lowest we’ll go

  private final WebClient http;
  private final ObjectMapper om = new ObjectMapper();

  @Value("${app.ocr.ocrspace.apikey:helloworld}") // demo key unless overridden
  private String apiKey;

  @Value("${app.ocr.ocrspace.language:eng}")
  private String language;

  @Value("${app.ocr.ocrspace.engine:2}")
  private int engine;

  public OcrSpaceOcrService(WebClient.Builder builder) {
    this.http = builder.baseUrl("https://api.ocr.space").build();
  }

  @Override
  public String extractText(MultipartFile file) throws Exception {
    String name = (file.getOriginalFilename() == null ? "scan" : file.getOriginalFilename()).toLowerCase();

    // 1) PDFs: try text layer first (no size limit hit)
    if (name.endsWith(".pdf")) {
      try (PDDocument doc = PDDocument.load(file.getBytes())) {
        String text = new PDFTextStripper().getText(doc).trim();
        if (!text.isBlank()) return text; // digital PDF; done

        // Scanned PDF → render each page and OCR
        PDFRenderer renderer = new PDFRenderer(doc);
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < doc.getNumberOfPages(); i++) {
          BufferedImage pageImg = renderer.renderImageWithDPI(i, PDF_RENDER_DPI, ImageType.RGB);
          byte[] jpeg = toSizedJpeg(pageImg);          // compress under ~1MB
          String pageText = callOcrSpace(jpeg, "page-" + (i+1) + ".jpg");
          if (!pageText.isBlank()) {
            all.append(pageText).append("\n\n");
          }
        }
        String out = all.toString().trim();
        return out.isBlank() ? "[OCR.space] No text found in scanned PDF." : out;
      }
    }

    // 2) Images: downscale + compress if needed
    String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
    if (ct.startsWith("image/") || name.matches(".*\\.(png|jpg|jpeg|webp|bmp)$")) {
      BufferedImage img = ImageIO.read(file.getInputStream());
      if (img == null) return "[OCR.space] Unsupported image format.";
      byte[] jpeg = toSizedJpeg(img);
      return callOcrSpace(jpeg, name.replaceAll("\\.[^.]+$", ".jpg"));
    }

    // 3) Fallback: try to send as-is (may fail for >1MB)
    byte[] raw = file.getBytes();
    if (raw.length > OCRSPACE_LIMIT_BYTES) {
      return "[OCR.space] File too large (>1MB) and not a supported image/PDF for automatic downscaling.";
    }
    return callOcrSpace(raw, name);
  }

  /* ---------- Helpers ---------- */

  private String callOcrSpace(byte[] bytes, String filename) throws Exception {
    // Ensure under the hard cap
    if (bytes.length > OCRSPACE_LIMIT_BYTES) {
      return "[OCR.space] Could not compress file under 1MB limit.";
    }
    var parts = new LinkedMultiValueMap<String, Object>();
    parts.add("language", language);
    parts.add("OCREngine", String.valueOf(engine));
    parts.add("isOverlayRequired", "false");
    parts.add("file", new ByteArrayResource(bytes) {
      @Override public String getFilename() { return filename; }
    });

    String json = http.post()
      .uri("/parse/image")
      .header("apikey", apiKey)
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(BodyInserters.fromMultipartData(parts))
      .retrieve()
      .bodyToMono(String.class)
      .timeout(Duration.ofSeconds(60))
      .block();

    if (json == null || json.isBlank()) return "[OCR.space] Empty response.";
    JsonNode root = om.readTree(json);

    if (root.path("IsErroredOnProcessing").asBoolean(false)) {
      String err;
      JsonNode em = root.path("ErrorMessage");
      if (em.isArray() && em.size() > 0) err = em.get(0).asText();
      else err = em.asText("[unknown]");
      return "[OCR.space] Error: " + err;
    }

    StringBuilder sb = new StringBuilder();
    for (JsonNode pr : root.path("ParsedResults")) {
      String text = pr.path("ParsedText").asText("");
      if (!text.isBlank()) sb.append(text).append("\n");
    }
    return sb.toString().trim();
  }

  private byte[] toSizedJpeg(BufferedImage src) throws Exception {
    // Downscale if needed
    BufferedImage img = downscaleIfNeeded(src, IMG_MAX_WIDTH, IMG_MAX_HEIGHT);

    // Try compressing quality from start → min until under 1MB
    float q = JPEG_START_QUALITY;
    byte[] out = toJpeg(img, q);
    while (out.length > OCRSPACE_LIMIT_BYTES && q > JPEG_MIN_QUALITY) {
      q -= 0.06f;
      out = toJpeg(img, q);
    }
    return out;
  }

  private static BufferedImage downscaleIfNeeded(BufferedImage src, int maxW, int maxH) {
    int w = src.getWidth(), h = src.getHeight();
    double scale = Math.min(1.0, Math.min((double) maxW / w, (double) maxH / h));
    if (scale >= 1.0) return src;
    int nw = (int) Math.round(w * scale), nh = (int) Math.round(h * scale);
    BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dst.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(src, 0, 0, nw, nh, null);
    g.dispose();
    return dst;
  }

  private static byte[] toJpeg(BufferedImage img, float quality) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
    if (!writers.hasNext()) throw new IllegalStateException("No JPEG writer found");
    ImageWriter writer = writers.next();
    try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
      writer.setOutput(ios);
      ImageWriteParam param = writer.getDefaultWriteParam();
      if (param.canWriteCompressed()) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Math.max(0.05f, Math.min(1f, quality)));
      }
      writer.write(null, new IIOImage(img, null, null), param);
    } finally {
      writer.dispose();
    }
    return baos.toByteArray();
  }
}
