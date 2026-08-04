package com.zhiplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.ExtractResultDTO;
import com.zhiplatform.entity.mysql.DocumentDraft;
import com.zhiplatform.repository.mysql.DocumentDraftRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档抽取草稿管理：抽取完成后先落库，切换页面不丢失，可稍后确认导入。
 */
@Service
public class DocumentDraftService {

    private final DocumentDraftRepository draftRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentDraftService(DocumentDraftRepository draftRepo) {
        this.draftRepo = draftRepo;
    }

    /**
     * 保存草稿并返回带 draftId 的结果。
     */
    public ExtractResultDTO saveDraft(ExtractResultDTO dto, Long userId) {
        DocumentDraft draft = new DocumentDraft();
        draft.setUserId(userId);
        draft.setFileName(dto.getFileName());
        draft.setSource(dto.getSource());
        draft.setCharCount(dto.getCharCount());
        draft.setEntityCount(dto.getEntities() == null ? 0 : dto.getEntities().size());
        draft.setRelationCount(dto.getRelations() == null ? 0 : dto.getRelations().size());
        try {
            draft.setDataJson(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new RuntimeException("草稿保存失败: " + e.getMessage(), e);
        }
        draft = draftRepo.save(draft);
        dto.setDraftId(draft.getId());
        return dto;
    }

    public List<DocumentDraft> listDrafts(Long userId) {
        return draftRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 加载草稿完整数据（校验归属）。
     */
    public ExtractResultDTO getDraft(Long id, Long userId) {
        DocumentDraft draft = draftRepo.findById(id).orElse(null);
        if (draft == null || !draft.getUserId().equals(userId)) return null;
        try {
            ExtractResultDTO dto = objectMapper.readValue(draft.getDataJson(), ExtractResultDTO.class);
            dto.setDraftId(draft.getId());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("草稿解析失败: " + e.getMessage(), e);
        }
    }

    public boolean deleteDraft(Long id, Long userId) {
        DocumentDraft draft = draftRepo.findById(id).orElse(null);
        if (draft == null || !draft.getUserId().equals(userId)) return false;
        draftRepo.deleteById(id);
        return true;
    }
}
