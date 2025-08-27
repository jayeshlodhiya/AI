package com.retailai.service;

import com.retailai.model.RagChunkNew;
import com.retailai.repo.RagChunkRepoNew;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.retailai.util.AnswerNormalizer.toPipedReader;

@Service
public class RagIngestService {

    private final RagChunkRepoNew repo;
    private final Path uploadRoot;
    private final ScanIngestService scan;


    public RagIngestService(RagChunkRepoNew repo, Path uploadRoot, ScanIngestService scan) {
        this.repo = repo;
        this.uploadRoot = uploadRoot;
        this.scan = scan;
    }

    public int ingestFile(MultipartFile file, String tenantId, String docType) throws IOException {
        // Save original file to disk first (already doing this per your previous code)
        Path saved = uploadRoot.resolve(Objects.requireNonNull(file.getOriginalFilename())).normalize();
        Files.createDirectories(saved.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, saved, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Extract text as a stream
        try (Reader reader = openTextReader(saved, file.getOriginalFilename(),file,tenantId,docType)) {
            final int CHUNK_SIZE = 2000;   // tune based on your embedding/tokenizer
            final int OVERLAP = 200;

            TextChunker.chunkStream(reader, CHUNK_SIZE, OVERLAP, chunkText -> {
                RagChunkNew chunk = new RagChunkNew();
                chunk.setTenantId(tenantId);
                chunk.setDocType(docType);          // or setDocumentType(...) to match your field name
                chunk.setContent(chunkText);
                // any extra metadata (file name, page range, etc.)
                repo.save(chunk); // write-through, avoids building big lists in RAM
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /**
     * Returns a Reader for text, handling PDFs/Docs page-wise to control memory.
     */
    private Reader openTextReader(Path path, String filename, MultipartFile file, String tenantId, String docType) throws Exception {
        String lower = filename.toLowerCase();
        PipedReader reader;
        if (lower.endsWith(".pdf")) {
            // PDF: extract text page-by-page into a Piped stream so we can consume as it’s produced
            return pdfReaderStreaming(path);
        } else if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")) {
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }else if( lower.endsWith(".xlsx")) {
            // Work from a byte[] so we can create multiple streams safely
            byte[] bytes = getBytes(file);
             reader = toPipedReader(extractTextFromExcel(new ByteArrayInputStream(bytes)));
            return reader;
        }else if(lower.endsWith(".jpg") || lower.endsWith(".png")){
            byte[] bytes = getBytes(file);
             reader = toPipedReader(extractFromImg(new ByteArrayInputStream(bytes),tenantId,docType,"",file));
            return reader;
        } else {
            // fallback to Apache Tika if you have it; otherwise treat as UTF-8 text
            return Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }
    }

    private static byte[] getBytes(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bytes;
    }

    // Example PDF streaming: PDFBox page-by-page
    private Reader pdfReaderStreaming(Path pdfPath) throws IOException {
        try {
            org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfPath.toFile());
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();

            PipedWriter writer = new PipedWriter();
            PipedReader reader = new PipedReader(writer, 64 * 1024);

            // Produce on a background thread; consumer reads via chunkStream
            new Thread(() -> {
                try (doc) {
                    int pages = doc.getNumberOfPages();
                    for (int p = 1; p <= pages; p++) {
                        stripper.setStartPage(p);
                        stripper.setEndPage(p);
                        String pageText = stripper.getText(doc);
                        writer.write(pageText);
                        writer.write('\n'); // page separator
                        writer.flush();
                    }
                } catch (IOException e) {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                    }
                } finally {
                    try {
                        writer.close();
                    } catch (IOException ignored) {
                    }
                }
            }, "pdf-chunker-" + pdfPath.getFileName()).start();

            return reader;
        } catch (IOException e) {
            // If PDF fails, fall back to empty
            return new StringReader("");
        }
    }
    // entry point from controller
    public Map<String,Object> ingestMultipart(MultipartFile mf, String tenantId, String type) {
        String filename = Optional.ofNullable(mf.getOriginalFilename()).orElse("upload");
        String ext = extOf(filename);

        // Work from a byte[] so we can create multiple streams safely
        byte[] bytes;
        try {
            bytes = mf.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String text;
        try {
            switch (ext) {
                case "xlsx":
                case "xls":
                    text = extractTextFromExcel(new ByteArrayInputStream(bytes));
                    break;
                case "pdf":
                    text = extractTextFromPdf(new ByteArrayInputStream(bytes));
                    break;
                case "doc":
                case "docx":
                    text = extractTextWithTika(new ByteArrayInputStream(bytes)); // easiest for Word
                    break;
                case "png":
                case "jpg": case "jpeg": case "gif": case "bmp": case "tiff":
                    // If you have OCR wired; else skip or store as attachment record
                    text = extractFromImg(new ByteArrayInputStream(bytes),tenantId,type,"",mf);
                    break;
                default:
                    // Try Tika for anything else (CSV, TXT, etc.)
                    text = extractTextWithTika(new ByteArrayInputStream(bytes));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from " + filename + ": " + e.getMessage(), e);
        }

        // --- from here, DO YOUR EXISTING CHUNKING/UPSERT ---
        int chunks = chunkAndUpsert(tenantId, type, filename, text);

        return Map.of("ok", true, "docId", filename, "chunks", chunks);
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return (i>=0 ? name.substring(i+1).toLowerCase() : "");
    }

    // ---------- Excel text extraction ----------
    private String extractTextFromExcel(InputStream in) throws IOException {
        try (var wb = WorkbookFactory.create(in)) {
            StringBuilder sb = new StringBuilder();
            DataFormatter fmt = new DataFormatter(); // keeps what user sees (dates, numbers)
            for (Sheet sheet : wb) {
                sb.append("\n=== Sheet: ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(fmt.formatCellValue(cell));
                    }
                    // drop empty lines
                    if (cells.stream().anyMatch(s -> s != null && !s.isBlank())) {
                        sb.append(String.join("\t", cells)).append('\n');
                    }
                }
            }

            return sb.toString();
        }
    }

    // ---------- PDF ----------
    private String extractTextFromPdf(InputStream in) throws IOException {
        try (PDDocument doc = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    // ---------- Tika (generic) ----------
    private String extractTextWithTika(InputStream in) throws IOException, TikaException {
        // Tika handles CSV, TXT, many Office types; returns best-effort Unicode text
       Tika tika = new Tika();
        return tika.parseToString(in);

    }

    // ---------- OCR stub ----------
    private String extractFromImg(InputStream in, String tenantId, String type, String docId, MultipartFile mf) throws Exception {
        // If you have Tesseract or external OCR, call it here.
        // Otherwise return empty so you don't crash:
       Map<String, Object> data = scan.scanAndIndex(tenantId, type, docId, mf);
        return data.getOrDefault("data","No data extracted!").toString();
    }

    // ---------- your existing logic ----------
    private int chunkAndUpsert(String tenantId, String type, String filename, String text) {
        if (text == null) text = "";
        text = text.replace("\u0000", ""); // guard against strange control chars
        // ... your existing chunk(...) and repository upsert code ...
        // return numberOfChunks;
        return chunk(text, tenantId, type, filename);
    }

    private int chunk(String text, String tenantId, String type, String filename) {
        // your current chunking (make sure substring bounds are safe)
        // e.g., 800-1200 chars with overlap
        int count = 0;
        final int size = 1000, overlap = 120;
        for (int start = 0; start < text.length(); start += size - overlap) {
            int end = Math.min(text.length(), start + size);
            String piece = text.substring(start, end);
            // saveChunk(tenantId, filename, type, count, piece);
            count++;
            if (end >= text.length()) break;
        }
        return count;
    }
}