package com.zhiplatform.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档知识抽取结果（供教师确认后导入知识库）
 */
public class ExtractResultDTO {
    private String fileName;
    private String source;
    private int charCount;
    private String text;
    private Long draftId;
    private List<ExtractEntityDTO> entities = new ArrayList<>();
    private List<ExtractRelationDTO> relations = new ArrayList<>();

    public ExtractResultDTO() {}
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public int getCharCount() { return charCount; }
    public void setCharCount(int charCount) { this.charCount = charCount; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getDraftId() { return draftId; }
    public void setDraftId(Long draftId) { this.draftId = draftId; }
    public List<ExtractEntityDTO> getEntities() { return entities; }
    public void setEntities(List<ExtractEntityDTO> entities) { this.entities = entities; }
    public List<ExtractRelationDTO> getRelations() { return relations; }
    public void setRelations(List<ExtractRelationDTO> relations) { this.relations = relations; }
}

