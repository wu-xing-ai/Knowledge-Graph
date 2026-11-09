package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Favorite;
import com.zhiplatform.repository.mysql.ExerciseRepository;
import com.zhiplatform.repository.mysql.FavoriteRepository;
import com.zhiplatform.repository.mysql.MistakeRepository;
import com.zhiplatform.repository.mysql.NoteRepository;
import com.zhiplatform.repository.mysql.StudyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private ExerciseRepository exerciseRepo;
    @Mock
    private StudyRecordRepository recordRepo;
    @Mock
    private NoteRepository noteRepo;
    @Mock
    private FavoriteRepository favoriteRepo;
    @Mock
    private MistakeRepository mistakeRepo;

    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService = new ExerciseService(exerciseRepo, recordRepo, noteRepo, favoriteRepo, mistakeRepo);
    }

    @Test
    void addFavorite_savesWhenNotExists() {
        Favorite fav = new Favorite();
        fav.setUserId(1L);
        fav.setKnowledgeId(10L);
        fav.setKnowledgeName("牛顿第二定律");
        when(favoriteRepo.findByUserIdAndKnowledgeId(1L, 10L)).thenReturn(Optional.empty());
        when(favoriteRepo.save(fav)).thenReturn(fav);

        Favorite saved = exerciseService.addFavorite(fav);
        assertEquals("牛顿第二定律", saved.getKnowledgeName());
        verify(favoriteRepo).save(fav);
    }

    @Test
    void addFavorite_returnsExistingWhenDuplicated() {
        Favorite existing = new Favorite();
        existing.setId(5L);
        existing.setUserId(1L);
        existing.setKnowledgeId(10L);
        when(favoriteRepo.findByUserIdAndKnowledgeId(1L, 10L)).thenReturn(Optional.of(existing));

        Favorite dup = new Favorite();
        dup.setUserId(1L);
        dup.setKnowledgeId(10L);

        Favorite result = exerciseService.addFavorite(dup);
        // 幂等：重复收藏直接返回已存在记录，不再保存
        assertEquals(5L, result.getId());
        verify(favoriteRepo, never()).save(any());
    }

    @Test
    void addFavorite_savesDirectlyWhenKnowledgeIdNull() {
        Favorite fav = new Favorite();
        fav.setUserId(1L);
        when(favoriteRepo.save(fav)).thenReturn(fav);

        Favorite saved = exerciseService.addFavorite(fav);
        assertNotNull(saved);
        verify(favoriteRepo).save(fav);
    }

    @Test
    void deleteFavorite_callsRepository() {
        exerciseService.deleteFavorite(3L);
        verify(favoriteRepo).deleteById(3L);
    }

    @Test
    void getUserFavorites_returnsOrderedList() {
        Favorite f1 = new Favorite();
        when(favoriteRepo.findByUserIdOrderByCreateTimeDesc(1L)).thenReturn(List.of(f1));

        List<Favorite> favorites = exerciseService.getUserFavorites(1L);
        assertEquals(1, favorites.size());
    }
}
