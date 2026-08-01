package com.zhiplatform.service;

import com.zhiplatform.common.Result;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeService {

    private final KnowledgeNodeRepository knowledgeRepo;
    private final SubjectRepository subjectRepo;

    public KnowledgeService(KnowledgeNodeRepository knowledgeRepo, SubjectRepository subjectRepo) {
        this.knowledgeRepo = knowledgeRepo;
        this.subjectRepo = subjectRepo;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepo.findAll();
    }

    public List<Subject> getSubjectsByType(String type) {
        return subjectRepo.findByType(type);
    }

    public Subject saveSubject(Subject subject) {
        return subjectRepo.save(subject);
    }

    public void deleteSubject(Long id) {
        subjectRepo.deleteById(id);
    }

    public KnowledgeNode addKnowledge(KnowledgeNode node) {
        node.setCreateTime(LocalDateTime.now());
        return knowledgeRepo.save(node);
    }

    public KnowledgeNode updateKnowledge(KnowledgeNode node) {
        return knowledgeRepo.save(node);
    }

    public void deleteKnowledge(Long id) {
        knowledgeRepo.deleteById(id);
    }

    public KnowledgeNode getKnowledge(Long id) {
        return knowledgeRepo.findById(id).orElse(null);
    }

    public List<KnowledgeNode> searchKnowledge(String keyword, Long subjectId, String type) {
        if (subjectId != null) {
            return knowledgeRepo.findBySubjectId(subjectId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            return knowledgeRepo.findByNameContaining(keyword);
        }
        if (type != null) {
            return knowledgeRepo.findByType(type);
        }
        return knowledgeRepo.findAll();
    }

    public List<KnowledgeNode> getCrossKnowledge() {
        return knowledgeRepo.findByIsCross(true);
    }

    public int batchImport(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            KnowledgeNode node = new KnowledgeNode();
            node.setName(getCellString(row.getCell(0)));
            node.setType(getCellString(row.getCell(1)));
            node.setSubject(getCellString(row.getCell(2)));
            node.setDifficulty((int) getCellNumeric(row.getCell(3)));
            node.setDescription(getCellString(row.getCell(4)));
            node.setTags(getCellString(row.getCell(5)));
            node.setIsCross(Boolean.parseBoolean(getCellString(row.getCell(6))));
            node.setCreateTime(LocalDateTime.now());
            knowledgeRepo.save(node);
            count++;
        }
        workbook.close();
        return count;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) {
            // 公式单元格：优先读取 Excel 缓存的公式计算结果（与旧实现 setCellType(STRING) 行为一致），
            // 避免把 "=1+2" 之类的公式文本直接写入知识库。
            return getCachedFormulaValue(cell);
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String getCachedFormulaValue(Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> NumberToTextConverter.toText(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> cell.getCellFormula();
            };
        } catch (IllegalStateException e) {
            // 无缓存值时回退到公式文本
            return cell.getCellFormula();
        }
    }

    private double getCellNumeric(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        try {
            return Double.parseDouble(getCellString(cell));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
