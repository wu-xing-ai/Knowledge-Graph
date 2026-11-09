package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.Mistake;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.repository.mysql.ExerciseRepository;
import com.zhiplatform.repository.mysql.FavoriteRepository;
import com.zhiplatform.repository.mysql.MistakeRepository;
import com.zhiplatform.repository.mysql.NoteRepository;
import com.zhiplatform.repository.mysql.StudyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

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

    private Exercise exercise(Long id, String answer, String type, String subjectIds) {
        Exercise ex = new Exercise();
        ex.setId(id);
        ex.setContent("题目" + id);
        ex.setAnswer(answer);
        ex.setAnalysis("解析" + id);
        ex.setType(type);
        ex.setSubjectIds(subjectIds);
        return ex;
    }

    // ---------- getPracticeQuestions ----------

    @Test
    void getPracticeQuestions_filtersBySubject_andBlanksAnswer() {
        Exercise e1 = exercise(1L, "A", "CHOICE", "1,2");
        Exercise e2 = exercise(2L, "B", "CHOICE", "3");
        when(exerciseRepo.findAll()).thenReturn(List.of(e1, e2));

        List<Map<String, Object>> result = exerciseService.getPracticeQuestions(1L, 10);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
        assertNull(result.get(0).get("answer"));
        assertNull(result.get(0).get("analysis"));
        assertEquals("题目1", result.get(0).get("content"));
    }

    @Test
    void getPracticeQuestions_nullSubject_returnsAll_cappedByCount() {
        when(exerciseRepo.findAll()).thenReturn(List.of(
                exercise(1L, "A", "CHOICE", "1"),
                exercise(2L, "B", "CHOICE", "2"),
                exercise(3L, "C", "CHOICE", null)));

        List<Map<String, Object>> result = exerciseService.getPracticeQuestions(null, 2);
        assertEquals(2, result.size());
    }

    // ---------- submitPractice 判分 ----------

    @Test
    void submitPractice_correct_choiceCaseInsensitive() {
        Exercise ex = exercise(1L, "A", "CHOICE", "1");
        when(exerciseRepo.findById(1L)).thenReturn(Optional.of(ex));

        Map<String, Object> result = exerciseService.submitPractice(7L, 1L, " a ");
        assertEquals(Boolean.TRUE, result.get("correct"));
        assertEquals("A", result.get("answer"));
        assertEquals("解析1", result.get("analysis"));

        ArgumentCaptor<StudyRecord> captor = ArgumentCaptor.forClass(StudyRecord.class);
        verify(recordRepo).save(captor.capture());
        StudyRecord record = captor.getValue();
        assertEquals(7L, record.getUserId());
        assertEquals("PRACTICE", record.getActionType());
        assertEquals(1L, record.getTargetId());
        assertEquals("CHOICE", record.getTargetType());
        assertEquals("CORRECT", record.getExtra());

        verify(mistakeRepo, never()).save(any());
    }

    @Test
    void submitPractice_wrong_judgeType_writesRecordAndMistake() {
        Exercise ex = exercise(2L, "对", "JUDGE", "1");
        when(exerciseRepo.findById(2L)).thenReturn(Optional.of(ex));
        when(mistakeRepo.findByUserIdAndExerciseId(7L, 2L)).thenReturn(Optional.empty());

        Map<String, Object> result = exerciseService.submitPractice(7L, 2L, "错");
        assertEquals(Boolean.FALSE, result.get("correct"));

        ArgumentCaptor<StudyRecord> recordCaptor = ArgumentCaptor.forClass(StudyRecord.class);
        verify(recordRepo).save(recordCaptor.capture());
        assertEquals("WRONG", recordCaptor.getValue().getExtra());
        assertEquals("JUDGE", recordCaptor.getValue().getTargetType());

        ArgumentCaptor<Mistake> mistakeCaptor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeRepo).save(mistakeCaptor.capture());
        Mistake mistake = mistakeCaptor.getValue();
        assertEquals(7L, mistake.getUserId());
        assertEquals(2L, mistake.getExerciseId());
        assertEquals(1, mistake.getWrongCount());
        assertNotNull(mistake.getLastTime());
    }

    @Test
    void submitPractice_wrong_existingMistake_incrementsWrongCount() {
        Exercise ex = exercise(3L, "B", "CHOICE", "1");
        when(exerciseRepo.findById(3L)).thenReturn(Optional.of(ex));

        Mistake existing = new Mistake();
        existing.setId(9L);
        existing.setUserId(7L);
        existing.setExerciseId(3L);
        existing.setWrongCount(2);
        when(mistakeRepo.findByUserIdAndExerciseId(7L, 3L)).thenReturn(Optional.of(existing));

        Map<String, Object> result = exerciseService.submitPractice(7L, 3L, "C");
        assertEquals(Boolean.FALSE, result.get("correct"));

        ArgumentCaptor<Mistake> captor = ArgumentCaptor.forClass(Mistake.class);
        verify(mistakeRepo).save(captor.capture());
        assertEquals(9L, captor.getValue().getId());
        assertEquals(3, captor.getValue().getWrongCount());
    }

    @Test
    void submitPractice_exerciseNotFound_returnsNull() {
        when(exerciseRepo.findById(99L)).thenReturn(Optional.empty());
        assertNull(exerciseService.submitPractice(7L, 99L, "A"));
        verify(recordRepo, never()).save(any());
    }

    // ---------- getUserMistakes ----------

    @Test
    void getUserMistakes_joinsExercise() {
        Mistake m = new Mistake();
        m.setId(5L);
        m.setUserId(7L);
        m.setExerciseId(1L);
        m.setWrongCount(2);
        when(mistakeRepo.findByUserIdOrderByLastTimeDesc(7L)).thenReturn(List.of(m));
        when(exerciseRepo.findById(1L)).thenReturn(Optional.of(exercise(1L, "A", "CHOICE", "1")));

        List<Map<String, Object>> result = exerciseService.getUserMistakes(7L);
        assertEquals(1, result.size());
        Map<String, Object> item = result.get(0);
        assertEquals(5L, item.get("id"));
        assertEquals(1L, item.get("exerciseId"));
        assertEquals(2, item.get("wrongCount"));
        assertEquals("题目1", item.get("content"));
        assertEquals("CHOICE", item.get("type"));
    }

    @Test
    void removeMistake_deletesById() {
        exerciseService.removeMistake(5L);
        verify(mistakeRepo).deleteById(5L);
    }

    // ---------- getExerciseStats ----------

    @Test
    void getExerciseStats_aggregatesByTargetId() {
        StudyRecord r1 = practiceRecord(1L, 100L, "CORRECT");
        StudyRecord r2 = practiceRecord(2L, 100L, "WRONG");
        StudyRecord r3 = practiceRecord(1L, 100L, "CORRECT");
        StudyRecord r4 = practiceRecord(3L, 200L, "WRONG");
        when(recordRepo.findByActionType("PRACTICE")).thenReturn(List.of(r1, r2, r3, r4));

        List<Map<String, Object>> stats = exerciseService.getExerciseStats();
        assertEquals(2, stats.size());

        Map<String, Object> s100 = stats.stream()
                .filter(s -> s.get("exerciseId").equals(100L)).findFirst().orElseThrow();
        assertEquals(3, s100.get("attempts"));
        assertEquals(2, s100.get("userCount"));
        assertEquals(2, s100.get("correctCount"));
        assertEquals(67, ((Number) s100.get("correctRate")).intValue());

        Map<String, Object> s200 = stats.stream()
                .filter(s -> s.get("exerciseId").equals(200L)).findFirst().orElseThrow();
        assertEquals(1, s200.get("attempts"));
        assertEquals(1, s200.get("userCount"));
        assertEquals(0, ((Number) s200.get("correctRate")).intValue());
    }

    private StudyRecord practiceRecord(Long userId, Long targetId, String extra) {
        StudyRecord r = new StudyRecord();
        r.setUserId(userId);
        r.setActionType("PRACTICE");
        r.setTargetId(targetId);
        r.setTargetType("CHOICE");
        r.setExtra(extra);
        return r;
    }
}
