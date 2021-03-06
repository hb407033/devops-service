package io.choerodon.devops.infra.feign;

import java.util.List;

import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import io.choerodon.devops.infra.feign.fallback.AgileServiceClientFallback;

@FeignClient(value = "agile-service", fallback = AgileServiceClientFallback.class)
public interface AgileServiceClient {

    @GetMapping(value = "/v1/projects/{project_id}/issues/{issueId}")
    ResponseEntity<String> queryIssue(
            @PathVariable("project_id") Long projectId,
            @PathVariable("issueId") Long issueId,
            @RequestParam("organizationId") Long organizationId);

    @GetMapping(value = "/v1/projects/{project_id}/issues/query_issue_ids")
    ResponseEntity<String> queryIssues(@ApiParam(value = "项目id", required = true)
                                               @PathVariable(name = "project_id") Long projectId,
                                               @ApiParam(value = "issue编号", required = true)
                                               @RequestBody List<Long> issueIds);

    @GetMapping(value = "/v1/projects/{project_id}/sprint/active/{organization_id}")
    ResponseEntity<String> getActiveSprint(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "组织id", required = true)
            @PathVariable(value = "organization_id") Long organizationId);
}
