package com.zhiplatform.service;

import com.zhiplatform.dto.DocumentParseDTO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParseServiceTest {

    private final DocumentParseService service = new DocumentParseService(null);

    @Test
    void parse_txt_returnsUtf8Text() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "知识.txt", "text/plain",
                "光合作用是绿色植物利用光能转化有机物的过程。".getBytes(StandardCharsets.UTF_8));

        DocumentParseDTO dto = service.parse(file);

        assertEquals("知识.txt", dto.getFileName());
        assertEquals("本地解析(文本)", dto.getSource());
        assertTrue(dto.getText().contains("光合作用"));
        assertEquals(dto.getText().length(), dto.getCharCount());
    }

    @Test
    void parse_docx_extractsParagraphs() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText("催化剂能降低反应的活化能。");
            doc.write(out);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "化学.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                out.toByteArray());

        DocumentParseDTO dto = service.parse(file);

        assertEquals("本地解析(Word)", dto.getSource());
        assertTrue(dto.getText().contains("催化剂"));
        assertTrue(dto.getText().contains("活化能"));
    }

    @Test
    void parse_unsupportedType_throws() {
        // 含 NUL 字节、未知扩展名 -> 判定为无法识别
        MockMultipartFile file = new MockMultipartFile("file", "a.xyz", "application/octet-stream", new byte[]{0x00, 0x01, 0x02});
        assertThrows(IllegalArgumentException.class, () -> service.parse(file));
    }

    @Test
    void parse_pdfRenamedToDocx_isDetectedByMagicBytes() throws Exception {
        // 用 PDFBox 生成一个带文本层的 PDF，但命名为 .docx
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(72, 720);
            cs.showText("Photosynthesis converts CO2 and water into organic matter.");
            cs.endText();
            cs.close();
            doc.save(out);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "伪装.docx", "application/octet-stream", out.toByteArray());

        DocumentParseDTO dto = service.parse(file);

        assertEquals("本地解析(PDF文本层)", dto.getSource());
        assertTrue(dto.getText().contains("Photosynthesis"));
    }

    @Test
    void parse_ole2Doc_givesFriendlyError() {
        // 旧版 .doc 的 OLE2 魔数
        byte[] ole2 = new byte[]{ (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0 };
        MockMultipartFile file = new MockMultipartFile("file", "old.doc", "application/octet-stream", ole2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("旧版 Office"));
    }

    @Test
    void parse_textNamedDocx_isDetectedAsText() throws Exception {
        byte[] bytes = "这是一份被命名为docx的纯文本。".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "文本.docx", "text/plain", bytes);

        DocumentParseDTO dto = service.parse(file);

        assertEquals("本地解析(文本)", dto.getSource());
        assertTrue(dto.getText().contains("纯文本"));
    }
}
