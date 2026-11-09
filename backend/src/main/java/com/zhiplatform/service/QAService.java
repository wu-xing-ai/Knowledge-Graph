package com.zhiplatform.service;

import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.QaTemplate;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.QaTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QAService {
    private final KnowledgeNodeRepository knowledgeRepo;
    private final QaTemplateRepository templateRepo;
    private final ModelServiceClient llmClient;

    private static final String RAG_SYSTEM_PROMPT =
            "你是「知联汇学」交叉学科知识图谱平台的智能问答助手，面向高中师生。\n" +
            "请基于用户提供的【知识库上下文】回答问题，要求：\n" +
            "1. 准确、条理清晰、适合高中生理解；\n" +
            "2. 涉及跨学科问题时，明确说明学科之间的关联（如物理与化学的共通原理）；\n" +
            "3. 只依据知识库上下文作答，不得编造知识库中没有的事实；如果上下文不足，基于自身知识谨慎作答，不要提及知识库收录情况；\n" +
            "4. 回答末尾列出引用的知识点名称。";

    private static final Pattern THINK_BLOCK = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);
    private static final Set<String> STOPWORDS = Set.of(
            "什么", "如何", "怎么", "为什么", "哪些", "请问", "可以", "一下", "进行", "关于",
            "以及", "并且", "或者", "但是", "这个", "那个", "这些", "那些", "涉及", "关联",
            "结合", "分析", "内容", "意思", "区别", "作用", "原理", "解释", "说明", "了解");

    private static final Map<String, String> KEYWORD_MAPPING = new LinkedHashMap<>();
    static {
        KEYWORD_MAPPING.put("火箭|航天|卫星|太空|空间站", "航天拓展");
        KEYWORD_MAPPING.put("污染|水治理|环保|净化|废水", "环境科普");
        KEYWORD_MAPPING.put("力|速度|加速度|动能|势能|电流|磁场|光|热力学", "高中物理");
        KEYWORD_MAPPING.put("元素|化学反应|溶液|酸碱|氧化|有机|催化", "高中化学");
        KEYWORD_MAPPING.put("细胞|基因|蛋白质|酶|DNA|RNA|生态|光合作用|呼吸", "高中生物");
    }

    public QAService(KnowledgeNodeRepository knowledgeRepo, QaTemplateRepository templateRepo) {
        this(knowledgeRepo, templateRepo, null);
    }

    @Autowired
    public QAService(KnowledgeNodeRepository knowledgeRepo, QaTemplateRepository templateRepo,
                     ModelServiceClient llmClient) {
        this.knowledgeRepo = knowledgeRepo;
        this.templateRepo = templateRepo;
        this.llmClient = llmClient;
    }

    public QAResponse kbqa(QARequest request) {
        String question = request.getQuestion();
        List<String> keywords = extractKeywords(question);
        List<Long> relatedNodeIds = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        Set<KnowledgeNode> allResults = new LinkedHashSet<>();

        // 1) 问题中直接包含的知识点名称（最精准）
        allResults.addAll(knowledgeRepo.findByNameInQuestion(question));
        // 2) 关键词在名称中模糊匹配
        if (allResults.isEmpty()) {
            for (String keyword : keywords) {
                allResults.addAll(knowledgeRepo.findByNameContaining(keyword));
                if (allResults.size() >= 8) break;
            }
        }
        // 3) 关键词在名称/描述/标签中匹配
        if (allResults.isEmpty()) {
            for (String keyword : keywords) {
                allResults.addAll(knowledgeRepo.findByKeywordAnywhere(keyword));
                if (allResults.size() >= 8) break;
            }
        }

        // 仍无匹配 → 自动升级 AI 智能回答
        if (allResults.isEmpty()) {
            if (llmClient != null) {
                try {
                    String aiAnswer = llmClient.chat(RAG_SYSTEM_PROMPT,
                            "请直接、完整地回答高中生的这个问题，不要提及知识库收录情况：\n【问题】" + question);
                    aiAnswer = THINK_BLOCK.matcher(aiAnswer == null ? "" : aiAnswer).replaceAll("").trim();
                    return QAResponse.builder().answer(aiAnswer)
                            .relatedNodeIds(Collections.emptyList()).sources(Collections.emptyList())
                            .mode("kbqa_ai").build();
                } catch (Exception ignored) {
                    // 降级
                }
            }
            return QAResponse.builder().answer("抱歉，知识库中未找到相关知识点，且 AI 服务暂不可用。请换个方式提问或浏览知识图谱。")
                    .relatedNodeIds(Collections.emptyList()).sources(Collections.emptyList()).mode("kbqa").build();
        }

        StringBuilder answer = new StringBuilder();
        answer.append("根据知识库查询，为您找到以下相关信息：\n\n");
        int count = 0;
        for (KnowledgeNode node : allResults) {
            if (count >= 5) break;
            relatedNodeIds.add(node.getId()); sources.add(node.getName());
            answer.append("**").append(node.getName()).append("** [").append(node.getSubject()).append("]\n");
            if (node.getDescription() != null && !node.getDescription().isEmpty()) answer.append(node.getDescription()).append("\n");
            answer.append("\n"); count++;
        }
        List<String> matchedSubjects = identifySubjects(question);
        if (matchedSubjects.size() > 1) {
            answer.append("\n---\n**跨学科关联提示**：您的问题涉及").append(String.join("、", matchedSubjects)).append("等多个学科领域的知识。");
        }
        return QAResponse.builder().answer(answer.toString()).relatedNodeIds(relatedNodeIds).sources(sources).mode("kbqa").build();
    }

    /**
     * GraphRAG 增强问答：先在图谱中检索相关知识点作为上下文，再调用模型微服务生成答案。
     */
    public QAResponse graphRag(QARequest request) {
        if (llmClient == null) {
            return QAResponse.builder()
                    .answer("GraphRAG 服务未配置（缺少 MODELSCOPE_API_KEY）。")
                    .relatedNodeIds(Collections.emptyList()).sources(Collections.emptyList()).mode("graphrag").build();
        }
        String question = request.getQuestion();
        try {
            RagContext ctx = buildContext(question);
            String answer = llmClient.chat(RAG_SYSTEM_PROMPT, ctx.userPrompt);
            answer = THINK_BLOCK.matcher(answer == null ? "" : answer).replaceAll("").trim();
            return QAResponse.builder().answer(answer).relatedNodeIds(ctx.relatedNodeIds).sources(ctx.sources).mode("graphrag").build();
        } catch (Exception e) {
            return QAResponse.builder()
                    .answer("GraphRAG 回答失败：" + e.getMessage() + "\n\n可切换 KBQA 模式尝试。")
                    .relatedNodeIds(Collections.emptyList()).sources(Collections.emptyList()).mode("graphrag").build();
        }
    }

    /**
     * GraphRAG 流式问答：事件回调依次推送 meta（关联节点）、delta（增量文本）、done / error。
     */
    public void streamGraphRag(QARequest request, Consumer<QaStreamEvent> eventSink) {
        String question = request.getQuestion();
        try {
            if (llmClient == null) {
                eventSink.accept(new QaStreamEvent("meta", metaData(Collections.emptyList(), Collections.emptyList(), "graphrag")));
                eventSink.accept(new QaStreamEvent("delta", Map.of("content", "GraphRAG 服务未配置（缺少 MODELSCOPE_API_KEY）。")));
                eventSink.accept(new QaStreamEvent("done", Map.of()));
                return;
            }
            RagContext ctx = buildContext(question);
            eventSink.accept(new QaStreamEvent("meta", metaData(ctx.relatedNodeIds, ctx.sources, "graphrag")));
            llmClient.chatStream(RAG_SYSTEM_PROMPT, ctx.userPrompt, token ->
                    eventSink.accept(new QaStreamEvent("delta", Map.of("content", token))));
            eventSink.accept(new QaStreamEvent("done", Map.of()));
        } catch (Exception e) {
            eventSink.accept(new QaStreamEvent("error", Map.of("message", e.getMessage() == null ? e.toString() : e.getMessage())));
        }
    }

    /**
     * KBQA 流式问答：本地检索命中时逐段流式输出检索结果；未命中时走 AI 流式兜底。
     * 事件顺序：meta（关联节点 + mode）→ delta* → done / error。
     */
    public void streamKbqa(QARequest request, Consumer<QaStreamEvent> eventSink) {
        String question = request.getQuestion();
        try {
            List<String> keywords = extractKeywords(question);
            List<Long> relatedNodeIds = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            Set<KnowledgeNode> allResults = new LinkedHashSet<>();

            // 与 kbqa() 相同的三级检索
            allResults.addAll(knowledgeRepo.findByNameInQuestion(question));
            if (allResults.isEmpty()) {
                for (String keyword : keywords) {
                    allResults.addAll(knowledgeRepo.findByNameContaining(keyword));
                    if (allResults.size() >= 8) break;
                }
            }
            if (allResults.isEmpty()) {
                for (String keyword : keywords) {
                    allResults.addAll(knowledgeRepo.findByKeywordAnywhere(keyword));
                    if (allResults.size() >= 8) break;
                }
            }

            // 未命中 → AI 流式兜底
            if (allResults.isEmpty()) {
                if (llmClient != null) {
                    try {
                        eventSink.accept(new QaStreamEvent("meta",
                                metaData(Collections.emptyList(), Collections.emptyList(), "kbqa_ai")));
                        llmClient.chatStream(RAG_SYSTEM_PROMPT,
                                "请直接、完整地回答高中生的这个问题，不要提及知识库收录情况：\n【问题】" + question,
                                token -> eventSink.accept(new QaStreamEvent("delta", Map.of("content", token))));
                        eventSink.accept(new QaStreamEvent("done", Map.of()));
                        return;
                    } catch (Exception e) {
                        // AI 失败降级为本地提示
                    }
                }
                eventSink.accept(new QaStreamEvent("meta",
                        metaData(Collections.emptyList(), Collections.emptyList(), "kbqa")));
                streamText("抱歉，知识库中未找到相关知识点，且 AI 服务暂不可用。请换个方式提问或浏览知识图谱。",
                        chunk -> eventSink.accept(new QaStreamEvent("delta", Map.of("content", chunk))));
                eventSink.accept(new QaStreamEvent("done", Map.of()));
                return;
            }

            // 命中 → 组装检索结果并流式输出
            StringBuilder answer = new StringBuilder();
            answer.append("根据知识库查询，为您找到以下相关信息：\n\n");
            int count = 0;
            for (KnowledgeNode node : allResults) {
                if (count >= 5) break;
                relatedNodeIds.add(node.getId());
                sources.add(node.getName());
                answer.append("**").append(node.getName()).append("** [").append(node.getSubject()).append("]\n");
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    answer.append(node.getDescription()).append("\n");
                }
                answer.append("\n");
                count++;
            }
            List<String> matchedSubjects = identifySubjects(question);
            if (matchedSubjects.size() > 1) {
                answer.append("\n---\n**跨学科关联提示**：您的问题涉及")
                      .append(String.join("、", matchedSubjects)).append("等多个学科领域的知识。");
            }

            eventSink.accept(new QaStreamEvent("meta", metaData(relatedNodeIds, sources, "kbqa")));
            streamText(answer.toString(),
                    chunk -> eventSink.accept(new QaStreamEvent("delta", Map.of("content", chunk))));
            eventSink.accept(new QaStreamEvent("done", Map.of()));
        } catch (Exception e) {
            eventSink.accept(new QaStreamEvent("error",
                    Map.of("message", e.getMessage() == null ? e.toString() : e.getMessage())));
        }
    }

    /** 本地文本按小段流式输出（模拟打字效果），让 KBQA 也有逐字渲染体验。 */
    private void streamText(String text, Consumer<String> onChunk) throws InterruptedException {
        if (text == null || text.isEmpty()) return;
        int step = 12;
        for (int i = 0; i < text.length(); i += step) {
            onChunk.accept(text.substring(i, Math.min(i + step, text.length())));
            Thread.sleep(16L);
        }
    }

    public record QaStreamEvent(String name, Object data) {}

    private Map<String, Object> metaData(List<Long> relatedNodeIds, List<String> sources, String mode) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("relatedNodeIds", relatedNodeIds);
        meta.put("sources", sources);
        meta.put("mode", mode);
        return meta;
    }

    private RagContext buildContext(String question) {
        List<String> matchedSubjects = identifySubjects(question);
        List<String> keywords = extractKeywords(question);
        Set<KnowledgeNode> context = new LinkedHashSet<>();
        context.addAll(knowledgeRepo.findByNameInQuestion(question));
        if (context.size() < 12) {
            for (String keyword : keywords) {
                context.addAll(knowledgeRepo.findByNameContaining(keyword));
                context.addAll(knowledgeRepo.findByKeywordAnywhere(keyword));
                if (context.size() >= 12) break;
            }
        }
        if (context.isEmpty() && !matchedSubjects.isEmpty()) {
            for (String subject : matchedSubjects) {
                context.addAll(knowledgeRepo.findBySubject(subject));
                if (context.size() >= 12) break;
            }
        }

        List<Long> relatedNodeIds = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        StringBuilder kb = new StringBuilder();
        for (KnowledgeNode node : context) {
            if (kb.length() > 6000) break;
            kb.append("- ").append(node.getName()).append(" [").append(node.getSubject()).append("]：")
              .append(node.getDescription() == null ? "" : node.getDescription()).append("\n");
            relatedNodeIds.add(node.getId());
            sources.add(node.getName());
        }

        String userPrompt;
        if (kb.length() > 0) {
            userPrompt = "【知识库上下文】\n" + kb + "\n【问题】" + question +
                    "\n请结合知识库上下文回答，并在末尾列出引用的知识点名称。";
        } else {
            userPrompt = "请直接、完整地回答高中生的这个问题，不要提及知识库收录情况：\n【问题】" + question;
        }
        return new RagContext(userPrompt, relatedNodeIds, sources);
    }

    private static class RagContext {
        final String userPrompt;
        final List<Long> relatedNodeIds;
        final List<String> sources;
        RagContext(String userPrompt, List<Long> relatedNodeIds, List<String> sources) {
            this.userPrompt = userPrompt;
            this.relatedNodeIds = relatedNodeIds;
            this.sources = sources;
        }
    }

    public List<QaTemplate> getTemplates() { return templateRepo.findAll(); }
    public QaTemplate addTemplate(QaTemplate template) { return templateRepo.save(template); }

    private List<String> identifySubjects(String question) {
        List<String> subjects = new ArrayList<>();
        for (Map.Entry<String, String> entry : KEYWORD_MAPPING.entrySet())
            if (question.matches(".*(" + entry.getKey() + ").*")) subjects.add(entry.getValue());
        return subjects;
    }

    private List<String> extractKeywords(String question) {
        List<String> keywords = new ArrayList<>();
        // 先按标点/空白切分，再取 2+ 中文字符连续段；长词补 4 字滑动窗口提高召回
        for (String seg : question.split("[，。！？、；：,.!?;:\\s]+")) {
            if (seg.isEmpty()) continue;
            Matcher m = Pattern.compile("[\\u4e00-\\u9fa5]{2,}").matcher(seg);
            while (m.find()) {
                String w = m.group();
                if (!STOPWORDS.contains(w)) keywords.add(w);
                if (w.length() > 6) {
                    for (int i = 0; i + 4 <= w.length(); i++) {
                        String g = w.substring(i, i + 4);
                        if (!STOPWORDS.contains(g)) keywords.add(g);
                    }
                }
            }
        }
        return keywords;
    }
}

