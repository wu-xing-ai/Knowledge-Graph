package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.DocumentParseDTO;
import com.zhiplatform.dto.DocumentExtractJobDTO;
import com.zhiplatform.dto.ExtractResultDTO;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.DocumentDraft;
import com.zhiplatform.entity.mysql.DocumentImport;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.service.DocumentDraftService;
import com.zhiplatform.service.DocumentExtractJobService;
import com.zhiplatform.service.DocumentParseService;
import com.zhiplatform.service.KnowledgeExtractService;
import com.zhiplatform.service.KnowledgeService;
import com.zhiplatform.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final DocumentParseService documentParseService;
    private final DocumentExtractJobService documentExtractJobService;
    private final DocumentDraftService documentDraftService;
    private final UserService userService;

    public KnowledgeController(KnowledgeService knowledgeService,
                               DocumentParseService documentParseService,
                               DocumentExtractJobService documentExtractJobService,
                               DocumentDraftService documentDraftService,
                               UserService userService) {
        this.knowledgeService = knowledgeService;
        this.documentParseService = documentParseService;
        this.documentExtractJobService = documentExtractJobService;
        this.documentDraftService = documentDraftService;
        this.userService = userService;
    }

    // 学科管理
    @GetMapping("/subject/list")
    public Result<List<Subject>> getSubjects(@RequestParam(required = false) String type) {
        if (type != null) {
            return Result.success(knowledgeService.getSubjectsByType(type));
        }
        return Result.success(knowledgeService.getAllSubjects());
    }

    @PostMapping("/subject/add")
    public Result<Subject> addSubject(@RequestBody Subject subject) {
        return Result.success(knowledgeService.saveSubject(subject));
    }

    @DeleteMapping("/subject/{id}")
    public Result<Void> deleteSubject(@PathVariable Long id) {
        knowledgeService.deleteSubject(id);
        return Result.success();
    }

    // 知识点管理
    @PostMapping("/add")
    public Result<KnowledgeNode> addKnowledge(@RequestBody KnowledgeNode node) {
        return Result.success(knowledgeService.addKnowledge(node));
    }

    @PutMapping("/update")
    public Result<KnowledgeNode> updateKnowledge(@RequestBody KnowledgeNode node) {
        return Result.success(knowledgeService.updateKnowledge(node));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<KnowledgeNode> getKnowledge(@PathVariable Long id) {
        return Result.success(knowledgeService.getKnowledge(id));
    }

    @GetMapping("/query")
    public Result<List<KnowledgeNode>> queryKnowledge(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String type) {
        return Result.success(knowledgeService.searchKnowledge(keyword, subjectId, type));
    }

    @GetMapping("/cross")
    public Result<List<KnowledgeNode>> getCrossKnowledge() {
        return Result.success(knowledgeService.getCrossKnowledge());
    }

    @PostMapping("/batch-import")
    public Result<String> batchImport(@RequestParam("file") MultipartFile file) {
        try {
            int count = knowledgeService.batchImport(file);
            return Result.success("成功导入 " + count + " 个知识点");
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            if (e instanceof org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException) {
                return Result.error(400, "导入失败: 这是旧版 .xls 格式，请先在 Excel 中另存为 .xlsx 后再导入");
            }
            return Result.error(400, "导入失败: 文件不是有效的 Excel（仅支持 .xlsx 新格式，旧版 .xls 请先在 Excel 中另存为 .xlsx）");
        } catch (Exception e) {
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    // Excel 导入模板下载
    @GetMapping("/template/download")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        try {
            byte[] bytes = knowledgeService.createImportTemplate();
            String encoded = URLEncoder.encode("知识库导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // 文档解析（Word/PDF/图片 -> 文本）
    @PostMapping("/document/parse")
    public Result<DocumentParseDTO> parseDocument(@RequestParam("file") MultipartFile file) {
        try {
            DocumentParseDTO dto = documentParseService.parse(file);
            return Result.success(dto);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "文档解析失败: " + e.getMessage());
        }
    }

    // 文档知识抽取（异步任务）：立即返回 jobId，前端轮询 /document/extract/status/{jobId}
    @PostMapping("/document/extract")
    public Result<Map<String, Object>> extractDocument(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            Long userId = userService.getUserByUsername(principal.getName()).getId();
            String jobId = documentExtractJobService.submit(file, userId);
            return Result.success(Map.of("jobId", jobId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "文档抽取提交失败: " + e.getMessage());
        }
    }

    // 轮询抽取任务状态
    @GetMapping("/document/extract/status/{jobId}")
    public Result<DocumentExtractJobDTO> extractStatus(@PathVariable String jobId, Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        DocumentExtractJobDTO job = documentExtractJobService.getStatus(jobId, userId);
        return job == null ? Result.error(404, "任务不存在或无权访问") : Result.success(job);
    }

    // 待确认导入的草稿列表
    @GetMapping("/document/drafts")
    public Result<List<DocumentDraft>> listDrafts(Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        return Result.success(documentDraftService.listDrafts(userId));
    }

    // 加载草稿完整数据（恢复预览）
    @GetMapping("/document/draft/{id}")
    public Result<ExtractResultDTO> getDraft(@PathVariable Long id, Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        ExtractResultDTO dto = documentDraftService.getDraft(id, userId);
        return dto == null ? Result.error(404, "草稿不存在或无权访问") : Result.success(dto);
    }

    // 删除草稿
    @DeleteMapping("/document/draft/{id}")
    public Result<Boolean> deleteDraft(@PathVariable Long id, Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        return Result.success(documentDraftService.deleteDraft(id, userId));
    }

    // 确认导入抽取结果（实体 + 关系 -> Neo4j），成功后删除对应草稿
    @PostMapping("/document/import")
    public Result<Map<String, Object>> importDocument(@RequestBody ExtractResultDTO dto, Principal principal) {
        try {
            Long userId = userService.getUserByUsername(principal.getName()).getId();
            Map<String, Object> stats = knowledgeService.importExtracted(dto, userId);
            if (dto.getDraftId() != null) {
                documentDraftService.deleteDraft(dto.getDraftId(), userId);
            }
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }

    // 当前账号导入过的文档列表
    @GetMapping("/document/list")
    public Result<List<DocumentImport>> listDocuments(Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        return Result.success(knowledgeService.listDocuments(userId));
    }

    // 一键删除文档及其解析出的数据
    @DeleteMapping("/document/{id}")
    public Result<Boolean> deleteDocument(@PathVariable Long id, Principal principal) {
        Long userId = userService.getUserByUsername(principal.getName()).getId();
        boolean ok = knowledgeService.deleteDocument(id, userId);
        return ok ? Result.success(true) : Result.error(404, "文档不存在或无权删除");
    }
}






