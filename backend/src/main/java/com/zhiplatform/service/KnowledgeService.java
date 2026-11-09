package com.zhiplatform.service;

import com.zhiplatform.dto.ExtractEntityDTO;
import com.zhiplatform.dto.ExtractRelationDTO;
import com.zhiplatform.dto.ExtractResultDTO;
import com.zhiplatform.entity.mysql.DocumentImport;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.DocumentImportRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeService {

    private final KnowledgeNodeRepository knowledgeRepo;
    private final SubjectRepository subjectRepo;
    private final Neo4jClient neo4jClient;
    private final DocumentImportRepository docImportRepo;

    public KnowledgeService(KnowledgeNodeRepository knowledgeRepo, SubjectRepository subjectRepo) {
        this(knowledgeRepo, subjectRepo, null, null);
    }

    public KnowledgeService(KnowledgeNodeRepository knowledgeRepo, SubjectRepository subjectRepo, Neo4jClient neo4jClient) {
        this(knowledgeRepo, subjectRepo, neo4jClient, null);
    }

    @Autowired
    public KnowledgeService(KnowledgeNodeRepository knowledgeRepo, SubjectRepository subjectRepo,
                            Neo4jClient neo4jClient, DocumentImportRepository docImportRepo) {
        this.knowledgeRepo = knowledgeRepo;
        this.subjectRepo = subjectRepo;
        this.neo4jClient = neo4jClient;
        this.docImportRepo = docImportRepo;
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
        applySubjectId(node);
        node.setCreateTime(LocalDateTime.now());
        return knowledgeRepo.save(node);
    }

    public KnowledgeNode updateKnowledge(KnowledgeNode node) {
        applySubjectId(node);
        return knowledgeRepo.save(node);
    }

    private void applySubjectId(KnowledgeNode node) {
        if (node == null) return;
        if (node.getSubjectId() != null) {
            subjectRepo.findById(node.getSubjectId()).ifPresent(subject -> {
                if (node.getSubject() == null || node.getSubject().isBlank()) {
                    node.setSubject(subject.getName());
                }
            });
            return;
        }
        if (node.getSubject() == null || node.getSubject().isBlank()) return;
        subjectRepo.findByName(node.getSubject()).stream().findFirst()
                .ifPresent(subject -> node.setSubjectId(subject.getId()));
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

    /**
     * 生成 Excel 批量导入模板（含表头、示例行、填写说明），返回 xlsx 字节。
     */
    public byte[] createImportTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("知识点导入");
            String[] headers = {"名称", "类型", "学科", "难度", "描述", "标签", "是否跨学科", "关联知识点", "交叉关系"};

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 示例行（会被真实数据替换）
            String[] example = {
                    "污水处理中的化学沉淀法", "交叉应用案例", "环境科普", "3",
                    "通过投加絮凝剂与沉淀剂，使污水中重金属离子转化为沉淀去除",
                    "水处理,化学方法", "true", "化学氧化法;水污染类型", "协同应用"
            };
            Row row = sheet.createRow(1);
            for (int i = 0; i < example.length; i++) {
                row.createCell(i).setCellValue(example[i]);
            }

            int[] widths = {22, 14, 12, 8, 48, 16, 12, 30, 12};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // 填写说明
            Sheet help = wb.createSheet("填写说明");
            String[][] notes = {
                    {"列", "含义", "填写说明"},
                    {"名称", "知识点名称", "必填；同文件内不能重复（图数据库有唯一约束）"},
                    {"类型", "知识点类型", "概念 / 公式 / 实验 / 考点 / 拓展素材 / 交叉应用案例"},
                    {"学科", "所属学科", "高中物理 / 高中化学 / 高中生物 / 航天拓展 / 环境科普"},
                    {"难度", "难度等级", "1-5 的整数"},
                    {"描述", "知识点描述", "可选"},
                    {"标签", "标签", "可选；多个用英文逗号分隔"},
                    {"是否跨学科", "跨学科标记", "填 true 或 false"},
                    {"关联知识点", "关联的知识点名称", "可选；多个用英文分号 ; 分隔，按名称匹配已有知识点"},
                    {"交叉关系", "关系类型", "可选；对该行所有关联知识点统一生效，如：跨学科关联 / 共通原理 / 交叉应用 / 基于原理"},
                    {"注意", "", "第 1 行为表头不会被导入；示例行可删除；重复导入同名知识点会跳过，关系自动去重。"}
            };
            for (int i = 0; i < notes.length; i++) {
                Row nr = help.createRow(i);
                for (int j = 0; j < notes[i].length; j++) {
                    nr.createCell(j).setCellValue(notes[i][j]);
                }
            }
            help.setColumnWidth(0, 16 * 256);
            help.setColumnWidth(1, 30 * 256);
            help.setColumnWidth(2, 70 * 256);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
    /**
     * 将 LLM 抽取结果（教师确认后）写入知识库：实体按名称 MERGE，关系按名称 MERGE。
     * 返回统计信息。
     */
    public Map<String, Object> importExtracted(ExtractResultDTO dto, Long userId) {
        int createdNodes = 0, existingNodes = 0, createdRelations = 0, skippedRelations = 0;

        // 记录文档导入（用于一键删除该文档解析出的数据）
        Long docImportId = null;
        DocumentImport docImport = null;
        boolean trackDoc = dto.getFileName() != null && !dto.getFileName().isBlank() && docImportRepo != null;
        if (trackDoc) {
            docImport = new DocumentImport();
            docImport.setUserId(userId);
            docImport.setFileName(dto.getFileName());
            docImport.setNodeCount(0);
            docImport.setRelationCount(0);
            docImport = docImportRepo.save(docImport);
            docImportId = docImport.getId();
        }
        String sourceMarker = docImportId == null ? null : "来源文档:" + docImportId;
        // 平台支持的知识点类型（LLM 可能返回非标准类型，统一规范化）
        java.util.Set<String> allowedTypes = java.util.Set.of("概念", "公式", "实验", "考点", "拓展素材", "交叉应用案例");
        java.util.function.Function<String, String> normalizeType = t -> t == null || t.isBlank() || !allowedTypes.contains(t) ? "概念" : t;

        // 先确定参与关系的实体名（用于 isCross 标记）
        java.util.Set<String> relatedNames = new java.util.HashSet<>();
        if (dto.getRelations() != null) {
            for (ExtractRelationDTO r : dto.getRelations()) {
                if (r.getSource() != null) relatedNames.add(r.getSource().trim());
                if (r.getTarget() != null) relatedNames.add(r.getTarget().trim());
            }
        }

        // 1) 实体入库（按名称去重）
        Map<String, KnowledgeNode> byName = new LinkedHashMap<>();
        if (dto.getEntities() != null) {
            for (ExtractEntityDTO e : dto.getEntities()) {
                String name = e.getName() == null ? "" : e.getName().trim();
                if (name.isEmpty()) continue;
                KnowledgeNode node = knowledgeRepo.findByName(name).stream().findFirst().orElse(null);
                if (node == null) {
                    node = new KnowledgeNode();
                    node.setName(name);
                    node.setType(normalizeType.apply(e.getType()));
                    node.setSubject(e.getSubject() == null || e.getSubject().isBlank() ? "未分类" : e.getSubject());
                    applySubjectId(node);
                    node.setDescription(e.getDescription());
                    node.setTags(sourceMarker == null ? "文档导入" : sourceMarker);
                    node.setIsCross(relatedNames.contains(name) || !relatedNames.isEmpty());
                    node.setCreateTime(LocalDateTime.now());
                    node = knowledgeRepo.save(node);
                    createdNodes++;
                } else {
                    existingNodes++;
                }
                byName.put(name, node);
            }
        }

        // 2) 关系入库（MERGE 幂等）
        if (dto.getRelations() != null) {
            for (ExtractRelationDTO r : dto.getRelations()) {
                String sourceName = r.getSource() == null ? "" : r.getSource().trim();
                String targetName = r.getTarget() == null ? "" : r.getTarget().trim();
                KnowledgeNode a = byName.get(sourceName);
                KnowledgeNode b = byName.get(targetName);
                if (a == null || b == null || a.getId() == null || b.getId() == null) {
                    skippedRelations++;
                    continue;
                }
                String relationType = r.getRelationType() == null || r.getRelationType().isBlank()
                        ? "关联" : r.getRelationType().trim();
                String description = r.getDescription() == null ? "" : r.getDescription().trim();
                if (neo4jClient != null) {
                    try {
                        neo4jClient.query(
                                "MATCH (a:Knowledge) WHERE id(a) = $sourceId " +
                                "WITH a " +
                                "MATCH (b:Knowledge) WHERE id(b) = $targetId " +
                                "MERGE (a)-[rel:CROSS_RELATE {relationType: $relationType}]->(b) " +
                                "SET rel.description = $description")
                                .bindAll(Map.of(
                                        "sourceId", a.getId(),
                                        "targetId", b.getId(),
                                        "relationType", relationType,
                                        "description", description))
                                .run();
                        createdRelations++;
                    } catch (Exception ex) {
                        skippedRelations++;
                    }
                } else {
                    skippedRelations++;
                }
            }
        }

        if (docImport != null) {
            docImport.setNodeCount(createdNodes);
            docImport.setRelationCount(createdRelations);
            docImportRepo.save(docImport);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("createdNodes", createdNodes);
        stats.put("existingNodes", existingNodes);
        stats.put("createdRelations", createdRelations);
        stats.put("skippedRelations", skippedRelations);
        stats.put("docImportId", docImportId);
        return stats;
    }
    /**
     * Excel 批量导入知识点（可选支持交叉关系）：
     * 列 0-6：名称、类型、学科、难度、描述、标签、是否跨学科
     * 列 7（可选）：关联知识点（多个用 ; 或 、 分隔，按名称匹配）
     * 列 8（可选）：交叉关系类型（默认"跨学科关联"）
     */
    public int batchImport(MultipartFile file) throws IOException {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = getCellString(row.getCell(0));
            if (name == null || name.isBlank()) continue;

            // 幂等：同名节点已存在则跳过新建（关系仍会按 MERGE 保证存在），避免唯一约束冲突
            KnowledgeNode saved = knowledgeRepo.findByName(name).stream().findFirst().orElse(null);
            if (saved == null) {
                KnowledgeNode node = new KnowledgeNode();
                node.setName(name);
                node.setType(getCellString(row.getCell(1)));
                node.setSubject(getCellString(row.getCell(2)));
                applySubjectId(node);
                node.setDifficulty((int) getCellNumeric(row.getCell(3)));
                node.setDescription(getCellString(row.getCell(4)));
                node.setTags(getCellString(row.getCell(5)));
                node.setIsCross(Boolean.parseBoolean(getCellString(row.getCell(6))));
                node.setCreateTime(LocalDateTime.now());
                saved = knowledgeRepo.save(node);
                count++;
            }

            // 可选：导入关联知识点与交叉关系（列 7/8）
            String relatedNames = getCellString(row.getCell(7));
            if (relatedNames != null && !relatedNames.isBlank()) {
                String relationType = getCellString(row.getCell(8));
                if (relationType == null || relationType.isBlank()) relationType = "跨学科关联";
                createRelations(saved, relatedNames, relationType.trim());
            }
        }
        workbook.close();
        return count;
    }

    /**
     * 按名称匹配关联知识点并创建交叉关系（MERGE 幂等）。
     */
    void createRelations(KnowledgeNode source, String relatedNames, String relationType) {
        if (neo4jClient == null || source.getId() == null) return;
        String[] targets = relatedNames.split("[,;，；、]");
        for (String targetName : targets) {
            String t = targetName.trim();
            if (t.isEmpty() || t.equals(source.getName())) continue;
            try {
                neo4jClient.query(
                        "MATCH (a:Knowledge) WHERE id(a) = $sourceId " +
                        "WITH a " +
                        "MATCH (b:Knowledge) WHERE b.name = $targetName " +
                        "MERGE (a)-[r:CROSS_RELATE {relationType: $relationType}]->(b) " +
                        "SET r.description = $description")
                        .bindAll(Map.of(
                                "sourceId", source.getId(),
                                "targetName", t,
                                "relationType", relationType,
                                "description", source.getName() + " 与 " + t + " 建立" + relationType + "关联"))
                        .run();
            } catch (Exception e) {
                // 单个关联失败不影响整体导入
            }
        }
    }

    /**
     * 当前账号导入过的文档列表
     */
    public List<DocumentImport> listDocuments(Long userId) {
        return docImportRepo == null ? List.of() : docImportRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 一键删除某文档解析出的数据：删除带该文档来源标记的知识点节点及其关系。
     */
    public boolean deleteDocument(Long id, Long userId) {
        if (docImportRepo == null) return false;
        DocumentImport doc = docImportRepo.findById(id).orElse(null);
        if (doc == null || !doc.getUserId().equals(userId)) return false;
        if (neo4jClient != null) {
            try {
                neo4jClient.query(
                        "MATCH (n:Knowledge) " +
                        "WHERE ANY(t IN split(coalesce(n.tags, ''), ',') WHERE trim(t) = $marker) " +
                        "DETACH DELETE n")
                        .bindAll(Map.of("marker", "来源文档:" + id))
                        .run();
            } catch (Exception ignored) {
                // 图删除失败不阻塞记录删除
            }
        }
        docImportRepo.deleteById(id);
        return true;
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









