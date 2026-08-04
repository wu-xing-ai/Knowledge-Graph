package com.zhiplatform.service;

import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.springframework.data.neo4j.core.Neo4jClient;
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
import static org.mockito.ArgumentMatchers.anyMap;
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

    @Test
    void batchImport_createsRelationsFromOptionalColumns() throws Exception {
        Neo4jClient neo4jClient = mock(Neo4jClient.class);
        Neo4jClient.UnboundRunnableSpec unbound = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.RunnableSpec runnable = mock(Neo4jClient.RunnableSpec.class);
        when(neo4jClient.query(anyString())).thenReturn(unbound);
        when(unbound.bindAll(anyMap())).thenReturn(runnable);

        knowledgeService = new KnowledgeService(knowledgeRepo, subjectRepo, neo4jClient);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("知识");
            Row header = sheet.createRow(0);
            String[] headers = {"name", "type", "subject", "difficulty", "description", "tags", "isCross", "关联知识点", "交叉关系"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("水污染协同治理");
            row.createCell(1).setCellValue("交叉应用案例");
            row.createCell(2).setCellValue("环境科普");
            row.createCell(3).setCellValue(3);
            row.createCell(4).setCellValue("生物降解与化学氧化协同处理污水");
            row.createCell(5).setCellValue("协同治理");
            row.createCell(6).setCellValue("true");
            row.createCell(7).setCellValue("生物净化技术；化学氧化法");
            row.createCell(8).setCellValue("协同应用");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "relations.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            when(knowledgeRepo.save(any(KnowledgeNode.class))).thenAnswer(inv -> {
                KnowledgeNode n = inv.getArgument(0);
                n.setId(100L);
                return n;
            });

            int count = knowledgeService.batchImport(file);

            assertEquals(1, count);
            verify(unbound, times(2)).bindAll(anyMap());
            verify(runnable, times(2)).run();
        }
    }

    @Test
    void batchImport_isIdempotentWhenNameExists() throws Exception {
        Neo4jClient neo4jClient = mock(Neo4jClient.class);
        Neo4jClient.UnboundRunnableSpec unbound = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.RunnableSpec runnable = mock(Neo4jClient.RunnableSpec.class);
        when(neo4jClient.query(anyString())).thenReturn(unbound);
        when(unbound.bindAll(anyMap())).thenReturn(runnable);
        knowledgeService = new KnowledgeService(knowledgeRepo, subjectRepo, neo4jClient);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("知识");
            Row header = sheet.createRow(0);
            for (int i = 0; i < 9; i++) header.createCell(i).setCellValue("c" + i);
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("新能源电池");
            row.createCell(1).setCellValue("交叉应用案例");
            row.createCell(2).setCellValue("高中物理");
            row.createCell(3).setCellValue(4);
            row.createCell(4).setCellValue("描述");
            row.createCell(5).setCellValue("能源");
            row.createCell(6).setCellValue("true");
            row.createCell(7).setCellValue("氧化还原反应");
            row.createCell(8).setCellValue("共通原理");

            KnowledgeNode existing = new KnowledgeNode();
            existing.setId(26L);
            existing.setName("新能源电池");
            when(knowledgeRepo.findByName("新能源电池")).thenReturn(List.of(existing));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "again.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            int count = knowledgeService.batchImport(file);

            // 同名节点已存在：不新建（count=0），但关系仍保证存在（MERGE 被调用）
            assertEquals(0, count);
            verify(knowledgeRepo, never()).save(any(KnowledgeNode.class));
            verify(unbound, times(1)).bindAll(anyMap());
            verify(runnable, times(1)).run();
        }
    }
}