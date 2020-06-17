package io.choerodon.devops.app.service.impl;

import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.devops.api.vo.ApprovalVO;
import io.choerodon.devops.api.vo.LatestAppServiceVO;
import io.choerodon.devops.api.vo.UserAttrVO;
import io.choerodon.devops.app.service.DevopsGitService;
import io.choerodon.devops.app.service.UserAttrService;
import io.choerodon.devops.app.service.WorkDesktopService;
import io.choerodon.devops.infra.dto.AppServiceDTO;
import io.choerodon.devops.infra.dto.DevopsMergeRequestDTO;
import io.choerodon.devops.infra.dto.PipelineRecordDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.dto.iam.Tenant;
import io.choerodon.devops.infra.enums.ApprovalTypeEnum;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;
import io.choerodon.devops.infra.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class WorkDesktopServiceImpl implements WorkDesktopService {
    private static final String MERGE_REQUEST_CONTENT_FORMAT = "%s (%s)在应用服务“%s”中提交了合并请求";
    private static final String PIPELINE_CONTENT_FORMAT = "流水线 “%s” 目前暂停于【%s】阶段，需要您进行审核";
    private static final String ORGANIZATION_NAME_AND_PROJECT_NAME = "%s-%s";

    @Autowired
    private DevopsMergeRequestMapper devopsMergeRequestMapper;

    @Autowired
    private AppServiceMapper appServiceMapper;

    @Autowired
    private DevopsBranchMapper devopsBranchMapper;

    @Autowired
    private DevopsGitlabCommitMapper devopsGitlabCommitMapper;

    @Autowired
    private PipelineStageRecordMapper pipelineStageRecordMapper;

    @Autowired
    private UserAttrService userAttrService;

    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;

    @Autowired
    DevopsGitService devopsGitService;

    @Override
    public List<LatestAppServiceVO> listLatestAppService(Long organizationId, Long projectId) {
        Tenant tenant = baseServiceClientOperator.queryOrganizationById(organizationId);
        List<ProjectDTO> projectDTOList;
        if (projectId == null) {
            projectDTOList = baseServiceClientOperator.listIamProjectByOrgId(tenant.getTenantId());
        } else {
            projectDTOList = Collections.singletonList(baseServiceClientOperator.queryIamProjectById(projectId));
        }
        return listLatestUserAppServiceDTO(organizationId, projectDTOList);
    }

    @Override
    public List<ApprovalVO> listApproval(Long organizationId, Long projectId) {
        Tenant tenant = baseServiceClientOperator.queryOrganizationById(organizationId);

        if (projectId != null) {
            return listApprovalVOByProject(tenant, projectId);
        } else {
            List<ApprovalVO> approvalVOList = new ArrayList<>();
            List<ProjectDTO> projectList = baseServiceClientOperator.listIamProjectByOrgId(tenant.getTenantId());
            projectList.forEach(projectDTO -> {
                approvalVOList.addAll(listApprovalVOByProject(tenant, projectDTO.getId()));
            });
            return approvalVOList;
        }
    }

    private List<ApprovalVO> listApprovalVOByProject(Tenant tenant, Long projectId) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        String organizationAndProjectName = String.format(ORGANIZATION_NAME_AND_PROJECT_NAME, tenant.getTenantName(), projectDTO.getName());
        // 1.查询合并请求
        List<AppServiceDTO> appServiceDTOList = appServiceMapper.listByActive(projectId);
        approvalVOList.addAll(listMergeRequestApproval(organizationAndProjectName, appServiceDTOList));

        // 2.查出流水线请求
        approvalVOList.addAll(listPipelineApproval(organizationAndProjectName, projectId));
        return approvalVOList;
    }

    private List<ApprovalVO> listMergeRequestApproval(String organizationAndProjectName, List<AppServiceDTO> appServiceDTOList) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();
        List<Integer> gitlabProjectIds = appServiceDTOList.stream().map(AppServiceDTO::getGitlabProjectId).collect(Collectors.toList());
        Map<Integer, String> gitlabProjectAndAppNameMap = appServiceDTOList.stream().collect(Collectors.toMap(AppServiceDTO::getGitlabProjectId, AppServiceDTO::getName));
        // 查出该用户待审批的合并请求
        List<DevopsMergeRequestDTO> mergeRequestDTOList = new ArrayList<>();
        if (gitlabProjectIds.size() != 0) {
            mergeRequestDTOList = devopsMergeRequestMapper.listToBeAuditedByThisUserUnderProjectIds(gitlabProjectIds, DetailsHelper.getUserDetails() == null ? 0L : DetailsHelper.getUserDetails().getUserId());
        }
        // 根据authorId查出合并请求发起者信息
        Set<Long> authorIds = mergeRequestDTOList.stream().map(DevopsMergeRequestDTO::getAuthorId).collect(Collectors.toSet());
        List<UserAttrVO> userAttrDTOList = userAttrService.listUsersByGitlabUserIds(authorIds);
        List<Long> iamUserIds = userAttrDTOList.stream().map(UserAttrVO::getIamUserId).collect(Collectors.toList());
        Map<Long, List<IamUserDTO>> iamUserDTOMap = baseServiceClientOperator.queryUsersByUserIds(iamUserIds).stream().collect(Collectors.groupingBy(IamUserDTO::getId));
        Map<Long, List<UserAttrVO>> userAttrVO = userAttrDTOList.stream().collect(Collectors.groupingBy(UserAttrVO::getGitlabUserId));
        mergeRequestDTOList.forEach(devopsMergeRequestDTO -> {
            IamUserDTO iamUserDTO = iamUserDTOMap.get(userAttrVO.get(devopsMergeRequestDTO.getAuthorId()).get(0).getIamUserId()).get(0);
            ApprovalVO approvalVO = new ApprovalVO()
                    .setImageUrl(iamUserDTO.getImageUrl())
                    .setType(ApprovalTypeEnum.MERGE_REQUEST.getType())
                    .setOrganizationNameAndProjectName(organizationAndProjectName)
                    .setGitlabProjectId(devopsMergeRequestDTO.getGitlabProjectId().intValue())
                    .setContent(String.format(MERGE_REQUEST_CONTENT_FORMAT, iamUserDTO.getRealName(), iamUserDTO.getId(), gitlabProjectAndAppNameMap.get(devopsMergeRequestDTO.getGitlabProjectId().intValue())));
            approvalVOList.add(approvalVO);
        });

        return approvalVOList;
    }

    private List<ApprovalVO> listPipelineApproval(String organizationAndProjectName, Long projectId) {
        List<ApprovalVO> approvalVOList = new ArrayList<>();

        Long userId = DetailsHelper.getUserDetails().getUserId() == null ? 0 : DetailsHelper.getUserDetails().getUserId();
        // 查出该用户待审批的流水线阶段
        List<PipelineRecordDTO> pipelineRecordDTOList = pipelineStageRecordMapper.listToBeAuditedByProjectIds(Collections.singletonList(projectId), userId);
        List<PipelineRecordDTO> pipelineRecordDTOAuditByThisUserList = pipelineRecordDTOList.stream()
                .filter(pipelineRecordDTO -> pipelineRecordDTO.getAuditUser() != null && pipelineRecordDTO.getAuditUser().contains(String.valueOf(userId)))
                .collect(Collectors.toList());
        pipelineRecordDTOAuditByThisUserList.forEach(pipelineRecordDTO -> {
            ApprovalVO approvalVO = new ApprovalVO()
                    .setType(ApprovalTypeEnum.PIPE_LINE.getType())
                    .setOrganizationNameAndProjectName(organizationAndProjectName)
                    .setContent(String.format(PIPELINE_CONTENT_FORMAT, pipelineRecordDTO.getPipelineName(), pipelineRecordDTO.getStageName()))
                    .setPipelineId(pipelineRecordDTO.getPipelineId())
                    .setPipelineRecordId(pipelineRecordDTO.getId())
                    .setStageRecordId(pipelineRecordDTO.getStageRecordId())
                    .setTaskRecordId(pipelineRecordDTO.getTaskRecordId());
            approvalVOList.add(approvalVO);
        });
        return approvalVOList;
    }

    private List<LatestAppServiceVO> listLatestUserAppServiceDTO(Long organizationId, List<ProjectDTO> projectDTOList) {
        List<Long> projectIds = projectDTOList.stream().map(ProjectDTO::getId).collect(Collectors.toList());
        Map<Long, List<ProjectDTO>> projectDTOMap = projectDTOList.stream().collect(Collectors.groupingBy(ProjectDTO::getId));
        Long userId = DetailsHelper.getUserDetails().getUserId() == null ? 0 : DetailsHelper.getUserDetails().getUserId();
        List<LatestAppServiceVO> latestAppServiceVOList = new ArrayList<>();
        latestAppServiceVOList.addAll(appServiceMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsBranchMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsGitlabCommitMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));
        latestAppServiceVOList.addAll(devopsMergeRequestMapper.listLatestUseAppServiceIdAndDate(projectIds, userId));

        // 去掉重复的appService,只保留最近使用的
        List<LatestAppServiceVO> latestAppServiceVOListWithoutRepeatService = latestAppServiceVOList.stream().sorted(Comparator.comparing(LatestAppServiceVO::getLastUpdateDate).reversed())
                .filter(distinctByKey(LatestAppServiceVO::getLastUpdateDate))
                .collect(Collectors.toList());

        int end = Math.min(latestAppServiceVOListWithoutRepeatService.size(), 10);

        List<LatestAppServiceVO> latestTenAppServiceList = latestAppServiceVOListWithoutRepeatService.subList(0, end);

        Set<Long> appServiceIds = latestTenAppServiceList.stream().map(LatestAppServiceVO::getId).collect(Collectors.toSet());
        Map<Long, List<AppServiceDTO>> appServiceDTOMap = appServiceMapper.listAppServiceByIds(appServiceIds, null, null).stream().collect(Collectors.groupingBy(AppServiceDTO::getId));

        latestTenAppServiceList.forEach(latestAppServiceVO -> {
            AppServiceDTO appServiceDTO = appServiceDTOMap.get(latestAppServiceVO.getId()).get(0);
            ProjectDTO projectDTO = projectDTOMap.get(appServiceDTO.getProjectId()).get(0);
            latestAppServiceVO.setProjectName(projectDTO.getName())
                    .setProjectId(appServiceDTO.getProjectId())
                    .setCode(appServiceDTO.getCode())
                    .setName(appServiceDTO.getName());
        });
        return latestTenAppServiceList;
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}
