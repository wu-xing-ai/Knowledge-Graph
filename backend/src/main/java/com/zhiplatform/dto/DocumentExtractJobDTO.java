package com.zhiplatform.dto;

/**
 * 文档抽取异步任务状态（供前端轮询）
 */
public class DocumentExtractJobDTO {
    private String jobId;
    private Long userId;
    private String status;   // PARSING / EXTRACTING / SAVING / DONE / FAILED
    private int percent;
    private String message;
    private String error;
    private ExtractResultDTO result;

    public DocumentExtractJobDTO() {}

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPercent() { return percent; }
    public void setPercent(int percent) { this.percent = percent; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public ExtractResultDTO getResult() { return result; }
    public void setResult(ExtractResultDTO result) { this.result = result; }
}
