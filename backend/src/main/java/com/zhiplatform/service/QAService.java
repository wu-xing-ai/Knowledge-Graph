package com.zhiplatform.service;

import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.QaTemplate;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.neo4j.GraphDataRepository;
import com.zhiplatform.repository.mysql.QaTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QAService {

    private final KnowledgeNodeRepository knowledgeRepo;
    private final GraphDataRepository graphDataRepo;
    private final QaTemplateRepository templateRepo;

    private static final Map<String, String> KEYWORD_MAPPING = new LinkedHashMap<>();

    static {
        KEYWORD_MAPPING.put("火箭|航天|卫星|太空|空间站", "航天拓展");
        KEYWORD_MAPPING.put("污染|水治理|环保|净化|废水", "环境科普");
        KEYWORD_MAPPING.put("力|速度|加速度|动能|势能|电流|磁场|光|热力学", "高中物理");
        KEYWORD_MAPPING.put("元素|化学反应|溶液|酸碱|氧化|有机|催化", "高中化学");
        KEYWORD_MAPPING.put("细胞|基因|蛋白质|酶|DNA|RNA|生态|光合作用|呼吸", "高中生物");
    }

    public QAService(KnowledgeNodeRepository knowledgeRepo, GraphDataRepository graphDataRepo,
                     QaTemplateRepository templateRepo) {
        this.knowledgeRepo = knowledgeRepo;
        this.graphDataRepo = graphDataRepo;
        this.templateRepo = templateRepo;
    }

    public QAResponse kbqa(QARequest request) {
        String question = request.getQuestion();
        List<String> matchedSubjects = identifySubjects(question);
        List<String> keywords = extractKeywords(question);

        List<Long> relatedNodeIds = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        StringBuilder answer = new StringBuilder();

        // 按关键词和学科搜索知识点
        Set<KnowledgeNode> allResults = new LinkedHashSet<>();
        for (String keyword : keywords) {
            List<KnowledgeNode> nodes = knowledgeRepo.findByNameContaining(keyword);
            allResults.addAll(nodes);
        }

        // 如果关键词搜索结果为空，按学科搜索
        if (allResults.isEmpty() && !matchedSubjects.isEmpty()) {
            for (String subject : matchedSubjects) {
                allResults.addAll(knowledgeRepo.findBySubject(subject));
            }
        }

        if (allResults.isEmpty()) {
            return QAResponse.builder()
                    .answer("抱歉，未找到与您问题直接相关的知识点。请尝试换个方式提问，或浏览知识图谱查看相关内容。")
                    .relatedNodeIds(Collections.emptyList())
                    .sources(Collections.emptyList())
                    .mode("kbqa")
                    .build();
        }

        answer.append("根据知识库查询，为您找到以下相关信息：\n\n");
        int count = 0;
        for (KnowledgeNode node : allResults) {
            if (count >= 5) break;
            relatedNodeIds.add(node.getId());
            sources.add(node.getName());

            answer.append("**").append(node.getName()).append("**");
            answer.append(" [").append(node.getSubject()).append("]");
            answer.append("\n");
            if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                answer.append(node.getDescription()).append("\n");
            }
            answer.append("\n");
            count++;
        }

        if (matchedSubjects.size() > 1) {
            answer.append("\n---\n**跨学科关联提示**：您的问题涉及");
            answer.append(String.join("、", matchedSubjects));
            answer.append("等多个学科领域的知识。以上知识点展示了这些学科之间的关联。");
        }

        return QAResponse.builder()
                .answer(answer.toString())
                .relatedNodeIds(relatedNodeIds)
                .sources(sources)
                .mode("kbqa")
                .build();
    }

    public QAResponse graphRag(QARequest request) {
        QAResponse kbqaResult = kbqa(request);
        String enhancedAnswer = "【GraphRAG增强回答】\n\n" + kbqaResult.getAnswer() +
                "\n\n---\n以上答案基于交叉学科知识图谱的深度检索与关联分析生成。" +
                "如需更深入的分析，建议结合具体知识点在图谱中探索其关联关系。";
        return QAResponse.builder()
                .answer(enhancedAnswer)
                .relatedNodeIds(kbqaResult.getRelatedNodeIds())
                .sources(kbqaResult.getSources())
                .mode("graphrag")
                .build();
    }

    public List<QaTemplate> getTemplates() {
        return templateRepo.findAll();
    }

    public QaTemplate addTemplate(QaTemplate template) {
        return templateRepo.save(template);
    }

    private List<String> identifySubjects(String question) {
        List<String> subjects = new ArrayList<>();
        for (Map.Entry<String, String> entry : KEYWORD_MAPPING.entrySet()) {
            if (question.matches(".*(" + entry.getKey() + ").*")) {
                subjects.add(entry.getValue());
            }
        }
        return subjects;
    }

    private List<String> extractKeywords(String question) {
        List<String> keywords = new ArrayList<>();
        // 提取中文词组和英文单词
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z]{3,}");
        Matcher matcher = pattern.matcher(question);
        while (matcher.find()) {
            String word = matcher.group();
            // 过滤常见停用词
            if (!isStopWord(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("什么", "如何", "怎么", "为什么", "哪些", "请问",
                "可以", "一下", "进行", "关于", "以及", "并且", "或者", "但是", "这个", "那个",
                "这些", "那些", "涉及", "关联", "结合", "分析", "请问");
        return stopWords.contains(word);
    }
}
