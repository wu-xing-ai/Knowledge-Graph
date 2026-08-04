package com.zhiplatform.service;

import com.zhiplatform.dto.DocumentParseDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 文档解析服务：
 * - 按文件内容（魔数）识别真实类型，不轻信扩展名（如 PDF 改名 .docx 也能识别为 PDF）
 * - .docx 用 Apache POI 本地解析
 * - .txt/.md 直接读取
 * - .pdf 优先提取文本层；扫描型 PDF（无文本层）回退到云端 PaddleOCR
 * - 图片(.png/.jpg/.bmp/.tif/.webp/.gif) 走云端 PaddleOCR
 */
@Service
public class DocumentParseService {

    private static final Set<String> IMAGE_EXTS = Set.of("png", "jpg", "jpeg", "bmp", "tif", "tiff", "webp", "gif");
    private static final int PDF_MIN_TEXT_LENGTH = 30;

    private final OcrService ocrService;

    public DocumentParseService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public DocumentParseDTO parse(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        return parse(filename, file.getBytes());
    }

    public DocumentParseDTO parse(String filename, byte[] bytes) throws Exception {
        if (bytes.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }

        String ext = extension(filename).toLowerCase();
        String realType = detectRealType(bytes);

        // 按真实内容类型分派
        if ("pdf".equals(realType)) {
            return parsePdf(filename, bytes);
        }
        if ("docx".equals(realType)) {
            try {
                return parseDocx(filename, bytes);
            } catch (Exception e) {
                throw new IllegalArgumentException("docx 解析失败（文件可能已损坏，或实际是 .xlsx 表格）: " + e.getMessage());
            }
        }
        if ("ole2".equals(realType)) {
            throw new IllegalArgumentException("检测到旧版 Office 格式（.doc/.xls），请先在 Word/Excel 中另存为 .docx/.xlsx 后再上传");
        }
        if ("image".equals(realType)) {
            return parseImage(filename, bytes);
        }
        if ("text".equals(realType)) {
            return parseText(filename, bytes);
        }

        // 内容无法识别时按扩展名兜底
        if (IMAGE_EXTS.contains(ext)) {
            return parseImage(filename, bytes);
        }
        if ("txt".equals(ext) || "md".equals(ext)) {
            return parseText(filename, bytes);
        }
        throw new IllegalArgumentException("无法识别的文件类型（文件内容与扩展名不符，或格式不支持）。支持 docx/pdf/txt/md/常见图片");
    }

    private DocumentParseDTO parseDocx(String filename, byte[] bytes) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (t != null && !t.isBlank()) {
                    sb.append(t).append("\n");
                }
            }
            // 表格文本
            doc.getTables().forEach(table -> table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell -> {
                        String t = cell.getText();
                        if (t != null && !t.isBlank()) sb.append(t).append(" ");
                    })));
        }
        return toDto(filename, "本地解析(Word)", sb.toString());
    }

    private DocumentParseDTO parseText(String filename, byte[] bytes) {
        return toDto(filename, "本地解析(文本)", new String(bytes, StandardCharsets.UTF_8));
    }

    private DocumentParseDTO parsePdf(String filename, byte[] bytes) throws Exception {
        String text = null;
        try (PDDocument doc = PDDocument.load(bytes)) {
            if (!doc.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(doc);
            }
        } catch (Exception e) {
            // 解析失败视为扫描件，继续走 OCR
        }
        if (text != null && text.trim().length() >= PDF_MIN_TEXT_LENGTH) {
            return toDto(filename, "本地解析(PDF文本层)", text);
        }
        // 扫描型 PDF → 云端 OCR
        String ocr = ocrService.recognize(filename, bytes);
        return toDto(filename, "云端OCR(PaddleOCR)", ocr);
    }

    private DocumentParseDTO parseImage(String filename, byte[] bytes) throws Exception {
        String ocr = ocrService.recognize(filename, bytes);
        return toDto(filename, "云端OCR(PaddleOCR)", ocr);
    }

    private DocumentParseDTO toDto(String filename, String source, String text) {
        DocumentParseDTO dto = new DocumentParseDTO();
        dto.setFileName(filename);
        dto.setSource(source);
        dto.setText(text == null ? "" : text);
        dto.setCharCount(dto.getText().length());
        return dto;
    }

    private String extension(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1);
    }

    /**
     * 通过文件头魔数识别真实类型。
     */
    private String detectRealType(byte[] b) {
        if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') return "pdf";
        if (b.length >= 4 && (b[0] & 0xFF) == 0x50 && (b[1] & 0xFF) == 0x4B) return "docx"; // PK zip
        if (b.length >= 4 && (b[0] & 0xFF) == 0xD0 && (b[1] & 0xFF) == 0xCF
                && (b[2] & 0xFF) == 0x11 && (b[3] & 0xFF) == 0xE0) return "ole2"; // 旧版 Office
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image"; // jpg
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image";
        if (b.length >= 2 && (b[0] & 0xFF) == 0x42 && (b[1] & 0xFF) == 0x4D) return "image"; // bmp
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image"; // webp
        if (b.length >= 4 && ((b[0] == 'I' && b[1] == 'I' && (b[2] & 0xFF) == 42 && b[3] == 0)
                || (b[0] == 'M' && b[1] == 'M' && b[2] == 0 && (b[3] & 0xFF) == 42))) return "image"; // tiff
        if (isProbablyText(b)) return "text";
        return "unknown";
    }

    private boolean isProbablyText(byte[] b) {
        int n = Math.min(b.length, 1024);
        for (int i = 0; i < n; i++) {
            if ((b[i] & 0xFF) == 0) return false;
        }
        return n > 0;
    }
}

