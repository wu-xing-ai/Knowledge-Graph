package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.Favorite;
import com.zhiplatform.entity.mysql.Mistake;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.entity.mysql.Note;
import com.zhiplatform.repository.mysql.ExerciseRepository;
import com.zhiplatform.repository.mysql.FavoriteRepository;
import com.zhiplatform.repository.mysql.MistakeRepository;
import com.zhiplatform.repository.mysql.StudyRecordRepository;
import com.zhiplatform.repository.mysql.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepo;
    private final StudyRecordRepository recordRepo;
    private final NoteRepository noteRepo;
    private final FavoriteRepository favoriteRepo;
    private final MistakeRepository mistakeRepo;

    public ExerciseService(ExerciseRepository exerciseRepo,
                           StudyRecordRepository recordRepo, NoteRepository noteRepo,
                           FavoriteRepository favoriteRepo, MistakeRepository mistakeRepo) {
        this.exerciseRepo = exerciseRepo;
        this.recordRepo = recordRepo;
        this.noteRepo = noteRepo;
        this.favoriteRepo = favoriteRepo;
        this.mistakeRepo = mistakeRepo;
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

    // 问答历史：actionType='QA' 的记录按时间倒序
    public List<StudyRecord> getUserQaHistory(Long userId) {
        return recordRepo.findByUserIdAndActionTypeOrderByCreateTimeDesc(userId, "QA");
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

    // 收藏管理
    public Favorite addFavorite(Favorite favorite) {
        // 幂等：同一用户重复收藏同一知识点时直接返回已存在的记录
        if (favorite.getKnowledgeId() != null) {
            return favoriteRepo.findByUserIdAndKnowledgeId(favorite.getUserId(), favorite.getKnowledgeId())
                    .orElseGet(() -> favoriteRepo.save(favorite));
        }
        return favoriteRepo.save(favorite);
    }

    public void deleteFavorite(Long id) {
        favoriteRepo.deleteById(id);
    }

    public List<Favorite> getUserFavorites(Long userId) {
        return favoriteRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }

    // 刷题：按学科随机抽题，答案与解析置空避免泄露
    public List<Map<String, Object>> getPracticeQuestions(Long subjectId, int count) {
        List<Exercise> all = exerciseRepo.findAll();
        List<Exercise> pool = new ArrayList<>();
        for (Exercise ex : all) {
            if (subjectId == null || subjectIdsContain(ex.getSubjectIds(), subjectId)) {
                pool.add(ex);
            }
        }
        Collections.shuffle(pool);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Exercise ex : pool.subList(0, Math.min(count, pool.size()))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", ex.getId());
            item.put("content", ex.getContent());
            item.put("type", ex.getType());
            item.put("difficulty", ex.getDifficulty());
            item.put("subjectIds", ex.getSubjectIds());
            item.put("answer", null);
            item.put("analysis", null);
            result.add(item);
        }
        return result;
    }

    private boolean subjectIdsContain(String subjectIds, Long subjectId) {
        if (subjectIds == null || subjectIds.isBlank()) return false;
        String target = String.valueOf(subjectId);
        for (String s : subjectIds.split(",")) {
            if (s.trim().equals(target)) return true;
        }
        return false;
    }

    // 刷题提交：判分、写学习记录、答错 upsert 错题本
    public Map<String, Object> submitPractice(Long userId, Long exerciseId, String answer) {
        Exercise exercise = exerciseRepo.findById(exerciseId).orElse(null);
        if (exercise == null) {
            return null;
        }
        String std = exercise.getAnswer() == null ? "" : exercise.getAnswer().trim();
        String user = answer == null ? "" : answer.trim();
        boolean correct = !user.isEmpty() && std.equalsIgnoreCase(user);

        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setActionType("PRACTICE");
        record.setTargetId(exerciseId);
        record.setTargetType(exercise.getType());
        record.setExtra(correct ? "CORRECT" : "WRONG");
        recordRepo.save(record);

        if (!correct) {
            java.util.Optional<Mistake> existing = mistakeRepo.findByUserIdAndExerciseId(userId, exerciseId);
            Mistake mistake;
            if (existing.isPresent()) {
                mistake = existing.get();
                mistake.setWrongCount((mistake.getWrongCount() == null ? 0 : mistake.getWrongCount()) + 1);
            } else {
                // 新建错题，wrongCount 默认 1
                mistake = new Mistake();
                mistake.setUserId(userId);
                mistake.setExerciseId(exerciseId);
            }
            mistake.setLastTime(java.time.LocalDateTime.now());
            mistakeRepo.save(mistake);
        }
        // 答对时错题记录保留不动，由学生主动移除（标记掌握）

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("answer", exercise.getAnswer());
        result.put("analysis", exercise.getAnalysis());
        return result;
    }

    // 错题本：联习题补充题干信息
    public List<Map<String, Object>> getUserMistakes(Long userId) {
        List<Mistake> mistakes = mistakeRepo.findByUserIdOrderByLastTimeDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Mistake m : mistakes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("exerciseId", m.getExerciseId());
            item.put("wrongCount", m.getWrongCount());
            item.put("lastTime", m.getLastTime());
            Exercise ex = exerciseRepo.findById(m.getExerciseId()).orElse(null);
            if (ex != null) {
                item.put("content", ex.getContent());
                item.put("type", ex.getType());
                item.put("difficulty", ex.getDifficulty());
                item.put("answer", ex.getAnswer());
                item.put("analysis", ex.getAnalysis());
            }
            result.add(item);
        }
        return result;
    }

    public void removeMistake(Long id) {
        mistakeRepo.deleteById(id);
    }

    // 教师作答统计：按习题聚合 PRACTICE 记录
    public List<Map<String, Object>> getExerciseStats() {
        List<StudyRecord> records = recordRepo.findByActionType("PRACTICE");
        Map<Long, int[]> attemptsAndCorrect = new HashMap<>();
        Map<Long, java.util.Set<Long>> users = new HashMap<>();
        for (StudyRecord r : records) {
            Long targetId = r.getTargetId();
            if (targetId == null) continue;
            int[] ac = attemptsAndCorrect.computeIfAbsent(targetId, k -> new int[2]);
            ac[0]++;
            if ("CORRECT".equals(r.getExtra())) ac[1]++;
            users.computeIfAbsent(targetId, k -> new java.util.HashSet<>()).add(r.getUserId());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, int[]> entry : attemptsAndCorrect.entrySet()) {
            int attempts = entry.getValue()[0];
            int correct = entry.getValue()[1];
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("exerciseId", entry.getKey());
            item.put("attempts", attempts);
            item.put("userCount", users.getOrDefault(entry.getKey(), java.util.Set.of()).size());
            item.put("correctCount", correct);
            item.put("correctRate", attempts == 0 ? 0 : Math.round(correct * 100.0 / attempts));
            result.add(item);
        }
        return result;
    }
}
