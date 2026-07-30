package com.zhiplatform.dto;

import jakarta.validation.constraints.NotBlank;

public class QARequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    private String mode;

    public QARequest() {}

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
