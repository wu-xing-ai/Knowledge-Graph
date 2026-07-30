package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.entity.mysql.QaTemplate;
import com.zhiplatform.service.QAService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/qa")
public class QAController {

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/kbqa")
    public Result<QAResponse> kbqa(@Valid @RequestBody QARequest request) {
        return Result.success(qaService.kbqa(request));
    }

    @PostMapping("/graphrag")
    public Result<QAResponse> graphRag(@Valid @RequestBody QARequest request) {
        return Result.success(qaService.graphRag(request));
    }

    @GetMapping("/template/list")
    public Result<List<QaTemplate>> getTemplates() {
        return Result.success(qaService.getTemplates());
    }

    @PostMapping("/template/add")
    public Result<QaTemplate> addTemplate(@RequestBody QaTemplate template) {
        return Result.success(qaService.addTemplate(template));
    }
}
