package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.entity.mysql.GraphConfig;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.GraphService;
import com.zhiplatform.service.UserService;
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
class GraphControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private GraphService graphService;
    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        GraphController controller = new GraphController(graphService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getGraphData_success() throws Exception {
        GraphDataDTO dto = new GraphDataDTO();
        dto.setNodes(List.of());
        dto.setEdges(List.of());
        when(graphService.getGraphData(any(), any(), anyInt())).thenReturn(dto);

        mockMvc.perform(get("/api/graph/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void expandNode_success() throws Exception {
        GraphDataDTO dto = new GraphDataDTO();
        dto.setNodes(List.of());
        dto.setEdges(List.of());
        when(graphService.expandNode(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/graph/expand").param("nodeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void saveConfig_success() throws Exception {
        User user = new User();
        user.setId(1L);
        GraphConfig config = new GraphConfig();
        config.setName("配置");
        config.setUserId(1L);

        when(userService.getUserByUsername("teacher")).thenReturn(user);
        when(graphService.saveConfig(any(GraphConfig.class))).thenReturn(config);

        mockMvc.perform(post("/api/graph/config/save")
                        .principal(() -> "teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("配置"));
    }

    @Test
    void getConfigs_success() throws Exception {
        User user = new User();
        user.setId(1L);
        GraphConfig config = new GraphConfig();
        config.setId(1L);
        config.setName("配置");

        when(userService.getUserByUsername("teacher")).thenReturn(user);
        when(graphService.getUserConfigs(1L)).thenReturn(List.of(config));

        mockMvc.perform(get("/api/graph/config/list")
                        .principal(() -> "teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("配置"));
    }

    @Test
    void deleteConfig_success() throws Exception {
        mockMvc.perform(delete("/api/graph/config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(graphService).deleteConfig(1L);
    }
}
