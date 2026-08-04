package com.zhiplatform.dto;

/**
 * 文档解析结果：文件名 / 解析来源 / 提取文本
 */
public class DocumentParseDTO {

    private String fileName;
    private String source;
    private String text;
    private int charCount;

    public DocumentParseDTO() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getCharCount() { return charCount; }
    public void setCharCount(int charCount) { this.charCount = charCount; }
}
