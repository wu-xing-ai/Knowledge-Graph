package com.zhiplatform.dto;

import java.util.List;

public class QAResponse {

    private String answer;
    private List<Long> relatedNodeIds;
    private List<String> sources;
    private String mode;

    public QAResponse() {}

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Long> getRelatedNodeIds() { return relatedNodeIds; }
    public void setRelatedNodeIds(List<Long> relatedNodeIds) { this.relatedNodeIds = relatedNodeIds; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final QAResponse r = new QAResponse();
        public Builder answer(String v) { r.answer = v; return this; }
        public Builder relatedNodeIds(List<Long> v) { r.relatedNodeIds = v; return this; }
        public Builder sources(List<String> v) { r.sources = v; return this; }
        public Builder mode(String v) { r.mode = v; return this; }
        public QAResponse build() { return r; }
    }
}
