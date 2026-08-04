package com.zhiplatform.service;

import com.zhiplatform.dto.DocumentExtractJobDTO;
import com.zhiplatform.dto.DocumentParseDTO;
import com.zhiplatform.dto.ExtractResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文档抽取异步任务服务：
 * 上传后立即返回 jobId，后台执行 解析 -> LLM 抽取 -> 保存草稿，前端轮询真实进度。
 */
@Service
public class DocumentExtractJobService {

    private final DocumentParseService parseService;
    private final KnowledgeExtractService extractService;
    private final DocumentDraftService draftService;

    private final Map<String, DocumentExtractJobDTO> jobs = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "doc-extract");
        t.setDaemon(true);
        return t;
    });

    public DocumentExtractJobService(DocumentParseService parseService,
                                     KnowledgeExtractService extractService,
                                     DocumentDraftService draftService) {
        this.parseService = parseService;
        this.extractService = extractService;
        this.draftService = draftService;
    }

    /**
     * 提交抽取任务，返回 jobId。
     * 注意：在请求线程内先读取文件字节，避免异步线程访问被清理的临时文件。
     */
    public String submit(MultipartFile file, Long userId) throws Exception {
        String filename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        byte[] bytes = file.getBytes();
        String jobId = "job-" + seq.incrementAndGet();

        DocumentExtractJobDTO job = new DocumentExtractJobDTO();
        job.setJobId(jobId);
        job.setUserId(userId);
        job.setStatus("PARSING");
        job.setPercent(5);
        job.setMessage("任务已提交，开始解析文档...");
        jobs.put(jobId, job);

        executor.execute(() -> run(job, filename, bytes));
        return jobId;
    }

    private void run(DocumentExtractJobDTO job, String filename, byte[] bytes) {
        try {
            update(job, "PARSING", 15, "正在解析文档（扫描件将自动 OCR 识别）...");
            DocumentParseDTO parsed = parseService.parse(filename, bytes);

            update(job, "EXTRACTING", 60, "解析完成，AI 正在抽取知识点与交叉关系...");
            ExtractResultDTO result = extractService.extract(parsed);

            update(job, "SAVING", 90, "正在保存草稿...");
            result = draftService.saveDraft(result, job.getUserId());

            job.setResult(result);
            job.setStatus("DONE");
            job.setPercent(100);
            job.setMessage("抽取完成");
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setError(e.getMessage() == null ? "未知错误" : e.getMessage());
            job.setMessage("抽取失败");
        }
    }

    private void update(DocumentExtractJobDTO job, String status, int percent, String message) {
        job.setStatus(status);
        job.setPercent(percent);
        job.setMessage(message);
    }

    /**
     * 查询任务状态（校验归属）。
     */
    public DocumentExtractJobDTO getStatus(String jobId, Long userId) {
        DocumentExtractJobDTO job = jobs.get(jobId);
        if (job == null || !job.getUserId().equals(userId)) return null;
        return job;
    }
}
