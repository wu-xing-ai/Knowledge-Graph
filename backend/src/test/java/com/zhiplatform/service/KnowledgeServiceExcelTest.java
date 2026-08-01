package com.zhiplatform.service;

import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceExcelTest {

    @Mock
    private KnowledgeNodeRepository knowledgeRepo;
    @Mock
    private SubjectRepository subjectRepo;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(knowledgeRepo, subjectRepo);
    }

    @Test
    void batchImport_readsFormulaCellsFromCachedValue() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("知识");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("type");
            header.createCell(2).setCellValue("subject");
            header.createCell(3).setCellValue("difficulty");
            header.createCell(4).setCellValue("description");
            header.createCell(5).setCellValue("tags");
            header.createCell(6).setCellValue("isCross");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("公式知识点");
            row.createCell(1).setCellValue("概念");
            row.createCell(2).setCellValue("高中物理");
            // 数值公式：缓存结果为 4
            Cell diff = row.createCell(3);
            diff.setCellFormula("2+2");
            row.createCell(4).setCellValue("描述");
            row.createCell(5).setCellValue("标签");
            // 布尔公式：缓存结果为 TRUE
            Cell cross = row.createCell(6);
            cross.setCellFormula("1=1");

            // 用公式求值器写入缓存结果（保持单元格类型为 FORMULA）
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            when(knowledgeRepo.save(any(KnowledgeNode.class))).thenAnswer(inv -> inv.getArgument(0));

            int count = knowledgeService.batchImport(file);

            assertEquals(1, count);
            ArgumentCaptor<KnowledgeNode> captor = ArgumentCaptor.forClass(KnowledgeNode.class);
            verify(knowledgeRepo, times(1)).save(captor.capture());
            KnowledgeNode node = captor.getValue();
            assertEquals("公式知识点", node.getName());
            // 公式单元格应读到缓存计算值 4，而不是 "=2+2"（解析失败回退 0）
            assertEquals(4, node.getDifficulty());
            // 布尔公式应读到缓存值 true，而不是 "=1=1" 文本
            assertTrue(Boolean.TRUE.equals(node.getIsCross()));
        }
    }
}
