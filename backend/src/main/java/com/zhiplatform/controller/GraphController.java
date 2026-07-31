package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.entity.mysql.GraphConfig;
import com.zhiplatform.service.GraphService;
import com.zhiplatform.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;
    private final UserService userService;

    public GraphController(GraphService graphService, UserService userService) {
        this.graphService = graphService;
        this.userService = userService;
    }

    @GetMapping("/data")
    public Result<GraphDataDTO> getGraphData(
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(graphService.getGraphData(subjectIds, keyword, limit));
    }

    @GetMapping("/expand")
    public Result<GraphDataDTO> expandNode(@RequestParam Long nodeId) {
        return Result.success(graphService.expandNode(nodeId));
    }

    @PostMapping("/config/save")
    public Result<GraphConfig> saveConfig(@RequestBody GraphConfig config, Principal principal) {
        String username = principal.getName();
        Long userId = userService.getUserByUsername(username).getId();
        config.setUserId(userId);
        return Result.success(graphService.saveConfig(config));
    }

    @GetMapping("/config/list")
    public Result<List<GraphConfig>> getConfigs(Principal principal) {
        String username = principal.getName();
        Long userId = userService.getUserByUsername(username).getId();
        return Result.success(graphService.getUserConfigs(userId));
    }

    @DeleteMapping("/config/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        graphService.deleteConfig(id);
        return Result.success();
    }
}
