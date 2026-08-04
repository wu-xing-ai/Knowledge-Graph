package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.dto.RelationDTO;
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

    // ==================== 交叉关系管理 ====================

    @GetMapping("/relations")
    public Result<List<RelationDTO>> getRelations() {
        return Result.success(graphService.getRelations());
    }

    @PostMapping("/relation/add")
    public Result<Boolean> addRelation(@RequestBody RelationDTO dto) {
        boolean ok = graphService.addRelation(dto);
        return ok ? Result.success(true) : Result.error(400, "关系参数不合法（源/目标节点不能为空或相同）");
    }

    @DeleteMapping("/relation")
    public Result<Boolean> deleteRelation(@RequestParam Long sourceId,
                                          @RequestParam Long targetId,
                                          @RequestParam String relationType) {
        return Result.success(graphService.deleteRelation(sourceId, targetId, relationType));
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
