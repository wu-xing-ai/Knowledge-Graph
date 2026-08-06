package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.entity.mysql.QaTemplate;
import com.zhiplatform.service.QAService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /**
     * KBQA 流式问答（SSE）：本地检索命中时逐段流式输出，未命中时 AI 流式兜底。
     * 事件顺序同 GraphRAG：meta / delta* / done 或 error。
     */
    @PostMapping(value = "/kbqa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter kbqaStream(@Valid @RequestBody QARequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        CompletableFuture.runAsync(() -> {
            try {
                qaService.streamKbqa(request, event -> {
                    try {
                        emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
                    } catch (Exception ignored) {
                        // 客户端断开后忽略
                    }
                });
            } finally {
                // 事件流结束后主动关闭，避免连接挂到超时
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * GraphRAG 流式问答（SSE）：事件依次为 meta / delta* / done 或 error。
     */
    @PostMapping(value = "/graphrag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter graphRagStream(@Valid @RequestBody QARequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        CompletableFuture.runAsync(() -> {
            try {
                qaService.streamGraphRag(request, event -> {
                    try {
                        emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
                    } catch (Exception ignored) {
                        // 客户端断开后忽略
                    }
                });
            } finally {
                // 事件流结束后主动关闭，避免连接挂到超时
                emitter.complete();
            }
        });
        return emitter;
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
