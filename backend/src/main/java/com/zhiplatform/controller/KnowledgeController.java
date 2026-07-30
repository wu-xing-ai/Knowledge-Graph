package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
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
            return Result.success("成功导入 " + count + " 条知识点");
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }
}
