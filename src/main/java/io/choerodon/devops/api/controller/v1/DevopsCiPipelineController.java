package io.choerodon.devops.api.controller.v1;

import java.util.List;
import javax.validation.Valid;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.choerodon.core.iam.InitRoleCode;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.validator.DevopsCiPipelineAdditionalValidator;
import io.choerodon.devops.api.vo.CiCdPipelineVO;
import io.choerodon.devops.api.vo.DevopsCiPipelineVO;
import io.choerodon.devops.app.service.DevopsCiPipelineService;
import io.choerodon.devops.infra.dto.DevopsCiPipelineDTO;
import io.choerodon.swagger.annotation.Permission;

/**
 * 〈功能简述〉
 * 〈CI流水线Controller〉
 *
 * @author wanghao
 * @Date 2020/4/2 17:57
 */
@RestController
@RequestMapping("/v1/projects/{project_id}/cicd_pipelines")
public class DevopsCiPipelineController {

    private DevopsCiPipelineService devopsCiPipelineService;

    public DevopsCiPipelineController(DevopsCiPipelineService devopsCiPipelineService) {
        this.devopsCiPipelineService = devopsCiPipelineService;
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下创建ci流水线")
    @PostMapping
    public ResponseEntity<DevopsCiPipelineDTO> create(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @RequestBody @Valid CiCdPipelineVO ciCdPipelineVO) {
        DevopsCiPipelineAdditionalValidator.additionalCheckPipeline(ciCdPipelineVO);
        return ResponseEntity.ok(devopsCiPipelineService.create(projectId, ciCdPipelineVO));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "项目下更新ci流水线")
    @PutMapping("/{ci_pipeline_id}")
    public ResponseEntity<DevopsCiPipelineDTO> update(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "ci_pipeline_id") Long ciPipelineId,
            @RequestBody @Valid DevopsCiPipelineVO devopsCiPipelineVO) {
//        DevopsCiPipelineAdditionalValidator.additionalCheckPipeline(devopsCiPipelineVO);
        return ResponseEntity.ok(devopsCiPipelineService.update(projectId, ciPipelineId, devopsCiPipelineVO));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "查询cicd流水线配置")
    @GetMapping("/{cicd_pipeline_id}")
    public ResponseEntity<DevopsCiPipelineVO> query(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @ApiParam(value = "流水线Id", required = true)
            @PathVariable(value = "cicd_pipeline_id") Long ciPipelineId) {
        return ResponseEntity.ok(devopsCiPipelineService.query(projectId, ciPipelineId));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "查询项目下流水线")
    @GetMapping
    public ResponseEntity<List<DevopsCiPipelineVO>> listByProjectIdAndAppName(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.ok(devopsCiPipelineService.listByProjectIdAndAppName(projectId, name));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "停用流水线")
    @PutMapping("/{ci_pipeline_id}/disable")
    public ResponseEntity<DevopsCiPipelineDTO> disablePipeline(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @PathVariable(value = "ci_pipeline_id") Long ciPipelineId) {
        return ResponseEntity.ok(devopsCiPipelineService.disablePipeline(projectId, ciPipelineId));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "启用流水线")
    @PutMapping("/{ci_pipeline_id}/enable")
    public ResponseEntity<DevopsCiPipelineDTO> enablePipeline(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @PathVariable(value = "ci_pipeline_id") Long ciPipelineId) {
        return ResponseEntity.ok(devopsCiPipelineService.enablePipeline(projectId, ciPipelineId));
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_MEMBER, InitRoleCode.PROJECT_OWNER})
    @ApiOperation(value = "删除流水线")
    @DeleteMapping("/{ci_pipeline_id}")
    public ResponseEntity<Void> deletePipeline(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @PathVariable(value = "ci_pipeline_id") Long ciPipelineId) {
        devopsCiPipelineService.deletePipeline(projectId, ciPipelineId);
        return ResponseEntity.noContent().build();
    }

    @Permission(level = ResourceLevel.ORGANIZATION, roles = {InitRoleCode.PROJECT_OWNER, InitRoleCode.PROJECT_MEMBER})
    @ApiOperation(value = "全新执行GitLab流水线")
    @PostMapping(value = "/{ci_pipeline_id}/execute")
    public ResponseEntity<Boolean> executeNew(
            @PathVariable(value = "ci_pipeline_id") Long ciPipelineId,
            @ApiParam(value = "项目ID", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @RequestParam(value = "gitlab_project_id") Long gitlabProjectId,
            @ApiParam(value = "分支名", required = true)
            @RequestParam(value = "ref") String ref) {
        devopsCiPipelineService.executeNew(projectId, ciPipelineId, gitlabProjectId, ref);
        return ResponseEntity.noContent().build();
    }
}
