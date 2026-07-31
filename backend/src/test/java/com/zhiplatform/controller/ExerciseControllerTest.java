package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.entity.mysql.Exercise;
import com.zhiplatform.entity.mysql.Note;
import com.zhiplatform.entity.mysql.StudyRecord;
import com.zhiplatform.service.ExerciseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExerciseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        ExerciseController controller = new ExerciseController(exerciseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void addExercise_success() throws Exception {
        Exercise ex = new Exercise();
        ex.setId(1L);
        ex.setContent("题目");
        when(exerciseService.addExercise(any(Exercise.class))).thenReturn(ex);

        mockMvc.perform(post("/api/exercise/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ex)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("题目"));
    }

    @Test
    void getAllExercises_success() throws Exception {
        when(exerciseService.getAllExercises()).thenReturn(List.of(new Exercise()));

        mockMvc.perform(get("/api/exercise/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void saveRecord_success() throws Exception {
        StudyRecord record = new StudyRecord();
        record.setUserId(1L);
        record.setActionType("刷题");
        when(exerciseService.saveRecord(any(StudyRecord.class))).thenReturn(record);

        mockMvc.perform(post("/api/exercise/record/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(record)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getRecords_success() throws Exception {
        when(exerciseService.getUserRecords(1L)).thenReturn(List.of(new StudyRecord()));

        mockMvc.perform(get("/api/exercise/record/list").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addNote_success() throws Exception {
        Note note = new Note();
        note.setId(1L);
        note.setContent("笔记");
        when(exerciseService.addNote(any(Note.class))).thenReturn(note);

        mockMvc.perform(post("/api/exercise/note/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("笔记"));
    }

    @Test
    void getNotes_byCategory() throws Exception {
        Note note = new Note();
        note.setCategory("跨学科");
        when(exerciseService.getUserNotesByCategory(1L, "跨学科")).thenReturn(List.of(note));

        mockMvc.perform(get("/api/exercise/note/list")
                        .param("userId", "1")
                        .param("category", "跨学科"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
