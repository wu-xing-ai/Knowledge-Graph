package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.Note;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.repository.mysql.ExerciseRepository;
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
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepo;
    @Mock
    private StudyRecordRepository recordRepo;
    @Mock
    private NoteRepository noteRepo;

    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService = new ExerciseService(exerciseRepo, recordRepo, noteRepo);
    }

    @Test
    void addExercise_savesEntity() {
        Exercise ex = new Exercise();
        ex.setContent("题目");
        when(exerciseRepo.save(ex)).thenReturn(ex);

        Exercise saved = exerciseService.addExercise(ex);
        assertEquals("题目", saved.getContent());
        verify(exerciseRepo).save(ex);
    }

    @Test
    void updateExercise_savesEntity() {
        Exercise ex = new Exercise();
        ex.setId(1L);
        ex.setContent("更新");
        when(exerciseRepo.save(ex)).thenReturn(ex);

        Exercise updated = exerciseService.updateExercise(ex);
        assertEquals("更新", updated.getContent());
    }

    @Test
    void deleteExercise_callsRepository() {
        exerciseService.deleteExercise(3L);
        verify(exerciseRepo).deleteById(3L);
    }

    @Test
    void getExercise_returnsWhenFound() {
        Exercise ex = new Exercise();
        ex.setId(1L);
        when(exerciseRepo.findById(1L)).thenReturn(Optional.of(ex));

        Exercise found = exerciseService.getExercise(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getAllExercises_returnsList() {
        when(exerciseRepo.findAll()).thenReturn(List.of(new Exercise(), new Exercise()));
        assertEquals(2, exerciseService.getAllExercises().size());
    }

    @Test
    void saveRecord_savesRecord() {
        StudyRecord record = new StudyRecord();
        record.setUserId(1L);
        record.setActionType("刷题");
        when(recordRepo.save(record)).thenReturn(record);

        StudyRecord saved = exerciseService.saveRecord(record);
        assertEquals("刷题", saved.getActionType());
    }

    @Test
    void getUserRecords_returnsOrderedList() {
        StudyRecord r1 = new StudyRecord();
        when(recordRepo.findByUserIdOrderByCreateTimeDesc(1L)).thenReturn(List.of(r1));

        List<StudyRecord> records = exerciseService.getUserRecords(1L);
        assertEquals(1, records.size());
    }

    @Test
    void addNote_savesNote() {
        Note note = new Note();
        note.setUserId(1L);
        note.setContent("笔记内容");
        when(noteRepo.save(note)).thenReturn(note);

        Note saved = exerciseService.addNote(note);
        assertEquals("笔记内容", saved.getContent());
    }

    @Test
    void getUserNotesByCategory_filters() {
        Note note = new Note();
        note.setCategory("跨学科");
        when(noteRepo.findByUserIdAndCategory(1L, "跨学科")).thenReturn(List.of(note));

        List<Note> notes = exerciseService.getUserNotesByCategory(1L, "跨学科");
        assertEquals(1, notes.size());
    }
}
