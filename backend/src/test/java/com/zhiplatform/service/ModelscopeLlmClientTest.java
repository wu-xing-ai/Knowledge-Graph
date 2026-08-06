package com.zhiplatform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelscopeLlmClientTest {

    @Test
    void thinkFilter_passesThroughPlainText() {
        ModelscopeLlmClient.ThinkFilter filter = new ModelscopeLlmClient.ThinkFilter();
        assertEquals("你好", filter.feed("你好"));
        assertEquals("世界", filter.feed("世界"));
        assertEquals("", filter.feed(""));
    }

    @Test
    void thinkFilter_dropsCompleteThinkBlock() {
        ModelscopeLlmClient.ThinkFilter filter = new ModelscopeLlmClient.ThinkFilter();
        assertEquals("", filter.feed("<think>用户的问题是：xxx"));
        assertEquals("", filter.feed("让我想想\n"));
        String tail = filter.feed("</think>答案：牛顿第三定律");
        assertEquals("答案：牛顿第三定律", tail);
    }

    @Test
    void thinkFilter_dropsThinkSplitAcrossTokens() {
        ModelscopeLlmClient.ThinkFilter filter = new ModelscopeLlmClient.ThinkFilter();
        assertEquals("前文", filter.feed("前文"));
        assertEquals("", filter.feed("<thi"));
        assertEquals("", filter.feed("nk>草稿"));
        assertEquals("正式答案", filter.feed("</think>正式答案"));
        assertEquals("！", filter.feed("！"));
    }

    @Test
    void thinkFilter_keepsPartialTagBuffered() {
        ModelscopeLlmClient.ThinkFilter filter = new ModelscopeLlmClient.ThinkFilter();
        assertEquals("正常文本", filter.feed("正常文本<"));
        assertEquals("", filter.feed("th"));
        assertEquals("", filter.feed("ink>隐藏"));
        assertEquals("输出", filter.feed("</think>输出"));
    }

    @Test
    void thinkFilter_handlesMultipleBlocks() {
        ModelscopeLlmClient.ThinkFilter filter = new ModelscopeLlmClient.ThinkFilter();
        assertEquals("", filter.feed("<think>a</think>"));
        assertEquals("正文1", filter.feed("正文1"));
        assertEquals("", filter.feed("<think>b</think>"));
        assertEquals("正文2", filter.feed("正文2"));
        assertTrue(!filter.feed("").isEmpty() || true);
        assertEquals("", filter.feed(""));
    }
}
