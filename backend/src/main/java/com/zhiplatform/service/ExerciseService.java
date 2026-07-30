package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.entity.mysql.Note;
import com.zhiplatform.repository.mysql.ExerciseRepository;
import com.zhiplatform.repository.mysql.StudyRecordRepository;
import com.zhiplatform.repository.mysql.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepo;
    private final StudyRecordRepository recordRepo;
    private final NoteRepository noteRepo;

    public ExerciseService(ExerciseRepository exerciseRepo,
                           StudyRecordRepository recordRepo, NoteRepository noteRepo) {
        this.exerciseRepo = exerciseRepo;
        this.recordRepo = recordRepo;
        this.noteRepo = noteRepo;
    }

    // 习题管理
    public Exercise addExercise(Exercise exercise) {
        return exerciseRepo.save(exercise);
    }

    public Exercise updateExercise(Exercise exercise) {
        return exerciseRepo.save(exercise);
    }

    public void deleteExercise(Long id) {
        exerciseRepo.deleteById(id);
    }

    public Exercise getExercise(Long id) {
        return exerciseRepo.findById(id).orElse(null);
    }

    public List<Exercise> getAllExercises() {
        return exerciseRepo.findAll();
    }

    // 学习记录
    public StudyRecord saveRecord(StudyRecord record) {
        return recordRepo.save(record);
    }

    public List<StudyRecord> getUserRecords(Long userId) {
        return recordRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public List<StudyRecord> getUserRecordsByType(Long userId, String actionType) {
        return recordRepo.findByUserIdAndActionType(userId, actionType);
    }

    // 笔记管理
    public Note addNote(Note note) {
        return noteRepo.save(note);
    }

    public Note updateNote(Note note) {
        return noteRepo.save(note);
    }

    public void deleteNote(Long id) {
        noteRepo.deleteById(id);
    }

    public List<Note> getUserNotes(Long userId) {
        return noteRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public List<Note> getUserNotesByCategory(Long userId, String category) {
        return noteRepo.findByUserIdAndCategory(userId, category);
    }
}
