package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.entity.mysql.Note;
import com.zhiplatform.service.ExerciseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    // 习题
    @PostMapping("/add")
    public Result<Exercise> addExercise(@RequestBody Exercise exercise) {
        return Result.success(exerciseService.addExercise(exercise));
    }

    @PutMapping("/update")
    public Result<Exercise> updateExercise(@RequestBody Exercise exercise) {
        return Result.success(exerciseService.updateExercise(exercise));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteExercise(@PathVariable Long id) {
        exerciseService.deleteExercise(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<Exercise>> getAllExercises() {
        return Result.success(exerciseService.getAllExercises());
    }

    @GetMapping("/{id}")
    public Result<Exercise> getExercise(@PathVariable Long id) {
        return Result.success(exerciseService.getExercise(id));
    }

    // 学习记录
    @PostMapping("/record/save")
    public Result<StudyRecord> saveRecord(@RequestBody StudyRecord record) {
        return Result.success(exerciseService.saveRecord(record));
    }

    @GetMapping("/record/list")
    public Result<List<StudyRecord>> getRecords(@RequestParam Long userId) {
        return Result.success(exerciseService.getUserRecords(userId));
    }

    // 笔记
    @PostMapping("/note/add")
    public Result<Note> addNote(@RequestBody Note note) {
        return Result.success(exerciseService.addNote(note));
    }

    @PutMapping("/note/update")
    public Result<Note> updateNote(@RequestBody Note note) {
        return Result.success(exerciseService.updateNote(note));
    }

    @DeleteMapping("/note/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        exerciseService.deleteNote(id);
        return Result.success();
    }

    @GetMapping("/note/list")
    public Result<List<Note>> getNotes(@RequestParam Long userId,
                                        @RequestParam(required = false) String category) {
        if (category != null) {
            return Result.success(exerciseService.getUserNotesByCategory(userId, category));
        }
        return Result.success(exerciseService.getUserNotes(userId));
    }
}
