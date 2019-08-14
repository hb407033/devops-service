package io.choerodon.devops.app.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.kubernetes.client.JSON;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.choerodon.base.domain.PageRequest;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.DevopsClusterDTO;
import io.choerodon.devops.infra.dto.DevopsClusterProPermissionDTO;
import io.choerodon.devops.infra.dto.DevopsEnvPodDTO;
import io.choerodon.devops.infra.dto.DevopsEnvironmentDTO;
import io.choerodon.devops.infra.dto.iam.IamUserDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.feign.operator.IamServiceClientOperator;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsClusterMapper;
import io.choerodon.devops.infra.util.*;


@Service
public class DevopsClusterServiceImpl implements DevopsClusterService {

    @Value("${agent.version}")
    private String agentExpectVersion;

    @Value("${agent.serviceUrl}")
    private String agentServiceUrl;

    @Value("${agent.repoUrl}")
    private String agentRepoUrl;

    private JSON json = new JSON();

    @Autowired
    private IamServiceClientOperator iamServiceClientOperator;
    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private ClusterNodeInfoService clusterNodeInfoService;
    @Autowired
    private DevopsEnvPodService devopsEnvPodService;
    @Autowired
    private DevopsClusterMapper devopsClusterMapper;
    @Autowired
    private DevopsClusterProPermissionService devopsClusterProPermissionService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;


    @Override
    @Transactional
    public String createCluster(Long projectId, DevopsClusterReqVO devopsClusterReqVO) {

        // 插入记录
        DevopsClusterDTO devopsClusterDTO = ConvertUtils.convertObject(devopsClusterReqVO, DevopsClusterDTO.class);
        devopsClusterDTO.setToken(GenerateUUID.generateUUID());
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO = baseCreateCluster(devopsClusterDTO);

        devopsClusterDTO.setSkipCheckProjectPermission(true);

        IamUserDTO iamUserDTO = iamServiceClientOperator.queryUserByUserId(GitUserNameUtil.getUserId().longValue());

        // 渲染激活环境的命令参数
        InputStream inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterDTO.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterDTO.getToken());
        params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
        params.put("{CHOERODONID}", devopsClusterDTO.getChoerodonId());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CLUSTERID}", devopsClusterDTO
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    @Transactional
    public void updateCluster(Long clusterId, DevopsClusterReqVO devopsClusterReqVO) {
        List<Long> projects = devopsClusterReqVO.getProjects();
        Boolean skipCheckPro = devopsClusterReqVO.getSkipCheckProjectPermission();
        List<Long> addProjects = new ArrayList<>();
        DevopsClusterDTO devopsClusterDTO = baseQuery(clusterId);

        //以前不跳过项目权限校验,但是现在跳过，清空集群对应的项目集群校验表
        if (skipCheckPro && !devopsClusterDTO.getSkipCheckProjectPermission()) {
            devopsClusterProPermissionService.baseDeleteByClusterId(clusterId);
        } else {
            //操作集群项目权限校验表记录
            List<Long> projectIds = devopsClusterProPermissionService.baseListByClusterId(clusterId)
                    .stream().map(DevopsClusterProPermissionDTO::getProjectId).collect(Collectors.toList());

            projects.forEach(projectId -> {
                if (!projectIds.contains(projectId)) {
                    addProjects.add(projectId);
                } else {
                    projectIds.remove(projectId);
                }
            });
            addProjects.forEach(addProject -> {
                DevopsClusterProPermissionDTO devopsClusterProPermissionDTO = new DevopsClusterProPermissionDTO();
                devopsClusterProPermissionDTO.setClusterId(clusterId);
                devopsClusterProPermissionDTO.setProjectId(addProject);
                devopsClusterProPermissionService.baseInsertPermission(devopsClusterProPermissionDTO);
            });
            projectIds.forEach(deleteProject -> {
                DevopsClusterProPermissionDTO devopsClusterProPermissionDTO = new DevopsClusterProPermissionDTO();
                devopsClusterProPermissionDTO.setClusterId(clusterId);
                devopsClusterProPermissionDTO.setProjectId(deleteProject);
                devopsClusterProPermissionService.baseDeletePermission(devopsClusterProPermissionDTO);
            });
        }
        devopsClusterDTO = ConvertUtils.convertObject(devopsClusterReqVO, DevopsClusterDTO.class);
        devopsClusterDTO.setId(clusterId);
        baseUpdate(devopsClusterDTO);
    }

    @Override
    public void checkName(Long projectId, String name) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setName(name);
        baseCheckName(devopsClusterDTO);
    }

    @Override
    public PageInfo<ProjectReqVO> pageProjects(Long projectId, Long clusterId, PageRequest pageRequest,
                                               String[] params) {
        ProjectDTO iamProjectDTO = iamServiceClientOperator.queryIamProjectById(projectId);

        PageInfo<ProjectDTO> projectDTOPageInfo = iamServiceClientOperator
                .pageProjectByOrgId(iamProjectDTO.getOrganizationId(), pageRequest.getPage(), pageRequest.getSize(), null, params);
        PageInfo<ProjectReqVO> projectReqVOPageInfo = ConvertUtils.convertPage(projectDTOPageInfo, ProjectReqVO.class);
        List<ProjectReqVO> projectDTOS = new ArrayList<>();
        if (!projectDTOPageInfo.getList().isEmpty()) {
            List<Long> projectIds;
            if (clusterId != null) {
                projectIds = devopsClusterProPermissionService.baseListByClusterId(clusterId).stream()
                        .map(DevopsClusterProPermissionDTO::getProjectId).collect(Collectors.toList());
            } else {
                projectIds = new ArrayList<>();
            }
            projectDTOPageInfo.getList().forEach(projectDO -> {
                ProjectReqVO projectDTO = new ProjectReqVO(projectDO.getId(), projectDO.getName(), projectDO.getCode(), projectIds.contains(projectDO.getId()));
                projectDTOS.add(projectDTO);
            });
        }
        projectReqVOPageInfo.setList(projectDTOS);
        return projectReqVOPageInfo;
    }

    @Override
    public String queryShell(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = getDevopsClusterStatus(clusterId);
        InputStream inputStream;
        if (devopsClusterRepVO.getUpgrade()) {
            inputStream = this.getClass().getResourceAsStream("/shell/cluster-upgrade.sh");
        } else {
            inputStream = this.getClass().getResourceAsStream("/shell/cluster.sh");
        }

        //初始化渲染脚本
        IamUserDTO iamUserDTO = iamServiceClientOperator.queryUserByUserId(devopsClusterRepVO.getCreateBy());
        Map<String, String> params = new HashMap<>();
        params.put("{VERSION}", agentExpectVersion);
        params.put("{NAME}", "choerodon-cluster-agent-" + devopsClusterRepVO.getCode());
        params.put("{SERVICEURL}", agentServiceUrl);
        params.put("{TOKEN}", devopsClusterRepVO.getToken());
        params.put("{EMAIL}", iamUserDTO == null ? "" : iamUserDTO.getEmail());
        params.put("{REPOURL}", agentRepoUrl);
        params.put("{CHOERODONID}", devopsClusterRepVO.getChoerodonId());
        params.put("{CLUSTERID}", devopsClusterRepVO
                .getId().toString());
        return FileUtil.replaceReturnString(inputStream, params);
    }

    @Override
    public void checkCode(Long projectId, String code) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setCode(code);
        baseCheckCode(devopsClusterDTO);
    }

    @Override
    public PageInfo<ClusterWithNodesVO> pageClusters(Long projectId, Boolean doPage, PageRequest pageRequest, String params) {
        PageInfo<DevopsClusterRepVO> devopsClusterRepVOPageInfo = ConvertUtils.convertPage(basePageClustersByOptions(projectId, doPage, pageRequest, params), DevopsClusterRepVO.class);
        PageInfo<ClusterWithNodesVO> devopsClusterRepDTOPage = ConvertUtils.convertPage(devopsClusterRepVOPageInfo, ClusterWithNodesVO.class);

        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedEnvList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedEnvList();
        devopsClusterRepVOPageInfo.getList().forEach(devopsClusterRepVO -> {
            devopsClusterRepVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
            devopsClusterRepVO.setUpgrade(isUpdated(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
            if (devopsClusterRepVO.getUpgrade()) {
                devopsClusterRepVO.setUpgradeMessage("Version is too low, please upgrade!");
            }
        });

        devopsClusterRepDTOPage.setList(fromClusterE2ClusterWithNodesDTO(devopsClusterRepVOPageInfo.getList(), projectId));
        return devopsClusterRepDTOPage;
    }

    @Override
    public List<DevopsClusterBasicInfoVO> queryClustersAndNodes(Long projectId) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        List<DevopsClusterDTO> devopsClusterDTOList = devopsClusterMapper.select(devopsClusterDTO);
        List<DevopsClusterBasicInfoVO> devopsClusterBasicInfoVOList = ConvertUtils.convertList(devopsClusterDTOList, DevopsClusterBasicInfoVO.class);
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedEnvList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedEnvList();

        devopsClusterBasicInfoVOList.forEach(devopsClusterBasicInfoVO -> {
            devopsClusterBasicInfoVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterBasicInfoVO.getId()));
            devopsClusterBasicInfoVO.setUpgrade(isUpdated(connectedEnvList, updatedEnvList, devopsClusterBasicInfoVO.getId()));
            if (devopsClusterBasicInfoVO.getUpgrade()) {
                devopsClusterBasicInfoVO.setUpgradeMessage("Version is too low, please upgrade!");
            }
        });
        devopsClusterBasicInfoVOList.forEach(devopsClusterBasicInfoVO ->
                devopsClusterBasicInfoVO.setNodes(clusterNodeInfoService.queryNodeName(projectId, devopsClusterBasicInfoVO.getId())));

        return devopsClusterBasicInfoVOList;
    }

    @Override
    public List<ProjectReqVO> listClusterProjects(Long projectId, Long clusterId) {
        return devopsClusterProPermissionService.baseListByClusterId(clusterId).stream()
                .map(devopsClusterProPermissionDTO -> {
                    ProjectDTO projectDTO = iamServiceClientOperator.queryIamProjectById(devopsClusterProPermissionDTO.getProjectId());
                    return new ProjectReqVO(devopsClusterProPermissionDTO.getProjectId(), projectDTO.getName(), projectDTO.getCode(), null);
                }).collect(Collectors.toList());
    }

    public boolean isConnect(List<Long> connectedEnvList, List<Long> updatedEnvList, Long id) {
        if (connectedEnvList.contains(id)) {
            if (updatedEnvList.contains(id)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isUpdated(List<Long> connectedEnvList, List<Long> updatedEnvList, Long id) {
        if (connectedEnvList.contains(id)) {
            if (updatedEnvList.contains(id)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }


    @Override
    public void deleteCluster(Long clusterId) {
        baseDelete(clusterId);
    }

    @Override
    public Boolean checkConnectEnvs(Long clusterId) {
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedEnvList();
        List<DevopsEnvironmentDTO> devopsEnvironmentDTOS = devopsEnvironmentService.baseListByClusterId(clusterId);
        if (connectedEnvList.contains(clusterId) || !devopsEnvironmentDTOS.isEmpty()) {
            throw new CommonException("error.cluster.delete");
        }
        return true;
    }


    @Override
    public DevopsClusterRepVO query(Long clusterId) {
        return ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
    }

    @Override
    public PageInfo<DevopsClusterPodVO> pagePodsByNodeName(Long clusterId, String nodeName, PageRequest pageRequest, String searchParam) {
        PageInfo<DevopsEnvPodDTO> devopsEnvPodDTOPageInfo = basePageQueryPodsByNodeName(clusterId, nodeName, pageRequest, searchParam);
        PageInfo<DevopsClusterPodVO> clusterPodDTOPage = ConvertUtils.convertPage(devopsEnvPodDTOPageInfo, DevopsClusterPodVO.class);

        clusterPodDTOPage.setList(devopsEnvPodDTOPageInfo.getList().stream().map(this::podE2ClusterPodDTO).collect(Collectors.toList()));
        return clusterPodDTOPage;
    }

    @Override
    public DevopsClusterRepVO queryByCode(Long projectId, String code) {
        return ConvertUtils.convertObject(baseQueryByCode(projectId, code), DevopsClusterRepVO.class);
    }


    @Override
    public DevopsClusterDTO baseCreateCluster(DevopsClusterDTO devopsClusterDTO) {
        List<DevopsClusterDTO> devopsClusterDTOS = devopsClusterMapper.selectAll();
        String choerodonId = GenerateUUID.generateUUID().split("-")[0];
        if (!devopsClusterDTOS.isEmpty()) {
            devopsClusterDTO.setChoerodonId(devopsClusterDTOS.get(0).getChoerodonId());
        } else {
            devopsClusterDTO.setChoerodonId(choerodonId);
        }
        if (devopsClusterMapper.insert(devopsClusterDTO) != 1) {
            throw new CommonException("error.devops.cluster.insert");
        }
        return devopsClusterDTO;
    }

    @Override
    public void baseCheckName(DevopsClusterDTO devopsClusterDTO) {
        if (devopsClusterMapper.selectOne(devopsClusterDTO) != null) {
            throw new CommonException("error.cluster.name.exist");
        }
    }

    @Override
    public void baseCheckCode(DevopsClusterDTO devopsClusterDTO) {
        if (devopsClusterMapper.selectOne(devopsClusterDTO) != null) {
            throw new CommonException("error.cluster.code.exist");
        }
    }

    @Override
    public List<DevopsClusterDTO> baseListByProjectId(Long projectId, Long organizationId) {
        return devopsClusterMapper.listByProjectId(projectId, organizationId);
    }

    @Override
    public DevopsClusterDTO baseQuery(Long clusterId) {
        return devopsClusterMapper.selectByPrimaryKey(clusterId);
    }

    @Override
    public void baseUpdate(DevopsClusterDTO inputClusterDTO) {
        DevopsClusterDTO devopsClusterDTO = devopsClusterMapper.selectByPrimaryKey(inputClusterDTO.getId());
        inputClusterDTO.setObjectVersionNumber(devopsClusterDTO.getObjectVersionNumber());
        devopsClusterMapper.updateByPrimaryKeySelective(inputClusterDTO);
        devopsClusterMapper.updateSkipCheckPro(inputClusterDTO.getId(), inputClusterDTO.getSkipCheckProjectPermission());
    }

    @Override
    public PageInfo<DevopsClusterDTO> basePageClustersByOptions(Long projectId, Boolean doPage, PageRequest pageRequest, String params) {
        Map<String, Object> searchParamMap = TypeUtil.castMapParams(params);
        return PageHelper.startPage(pageRequest.getPage(), pageRequest.getSize(), PageRequestUtil.getOrderBy(pageRequest))
                .doSelectPageInfo(
                        () -> devopsClusterMapper.listClusters(
                                projectId,
                                TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                                TypeUtil.cast(searchParamMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public void baseDelete(Long clusterId) {
        devopsClusterMapper.deleteByPrimaryKey(clusterId);
    }

    @Override
    public DevopsClusterDTO baseQueryByToken(String token) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setToken(token);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public List<DevopsClusterDTO> baseList() {
        return devopsClusterMapper.selectAll();
    }

    @Override
    public PageInfo<DevopsEnvPodDTO> basePageQueryPodsByNodeName(Long clusterId, String nodeName, PageRequest pageRequest, String searchParam) {
        Map<String, Object> paramMap = TypeUtil.castMapParams(searchParam);
        return PageHelper.startPage(pageRequest.getPage(), pageRequest.getSize(), PageRequestUtil.getOrderBy(pageRequest)).doSelectPageInfo(() -> devopsClusterMapper.pageQueryPodsByNodeName(
                clusterId, nodeName,
                TypeUtil.cast(paramMap.get(TypeUtil.SEARCH_PARAM)),
                TypeUtil.cast(paramMap.get(TypeUtil.PARAMS))));
    }

    @Override
    public DevopsClusterDTO baseQueryByCode(Long projectId, String code) {
        DevopsClusterDTO devopsClusterDTO = new DevopsClusterDTO();
        devopsClusterDTO.setProjectId(projectId);
        devopsClusterDTO.setCode(code);
        return devopsClusterMapper.selectOne(devopsClusterDTO);
    }

    @Override
    public void baseUpdateProjectId(Long orgId, Long proId) {
        devopsClusterMapper.updateProjectId(orgId, proId);
    }

    /**
     * pod entity to cluster pod vo
     *
     * @param devopsEnvPodDTO pod entity
     * @return the cluster pod vo
     */
    private DevopsClusterPodVO podE2ClusterPodDTO(DevopsEnvPodDTO devopsEnvPodDTO) {
        DevopsClusterPodVO devopsClusterPodVO = new DevopsClusterPodVO();
        BeanUtils.copyProperties(devopsEnvPodDTO, devopsClusterPodVO);
        DevopsEnvPodVO devopsEnvPodVO = ConvertUtils.convertObject(devopsEnvPodDTO, DevopsEnvPodVO.class);
        devopsEnvPodService.setContainers(devopsEnvPodVO);

        devopsClusterPodVO.setContainersForLogs(
                devopsEnvPodDTO.getContainers()
                        .stream()
                        .map(container -> new DevopsEnvPodContainerLogVO(devopsEnvPodDTO.getName(), container.getName()))
                        .collect(Collectors.toList())
        );
        return devopsClusterPodVO;
    }

    /**
     * convert cluster entity to instances of {@link ClusterWithNodesVO}
     *
     * @param devopsClusterRepVOS the cluster entities
     * @param projectId           the project id
     * @return the instances of the return type
     */
    private List<ClusterWithNodesVO> fromClusterE2ClusterWithNodesDTO(List<DevopsClusterRepVO> devopsClusterRepVOS, Long projectId) {
        // default three records of nodes in the instance
        PageRequest pageRequest = new PageRequest(1, 3);

        return devopsClusterRepVOS.stream().map(cluster -> {
            ClusterWithNodesVO clusterWithNodesDTO = new ClusterWithNodesVO();
            BeanUtils.copyProperties(cluster, clusterWithNodesDTO);
            if (Boolean.TRUE.equals(clusterWithNodesDTO.getConnect())) {
                clusterWithNodesDTO.setNodes(clusterNodeInfoService.pageClusterNodeInfo(cluster.getId(), projectId, pageRequest));
            }
            return clusterWithNodesDTO;
        }).collect(Collectors.toList());
    }

    private DevopsClusterRepVO getDevopsClusterStatus(Long clusterId) {
        DevopsClusterRepVO devopsClusterRepVO = ConvertUtils.convertObject(baseQuery(clusterId), DevopsClusterRepVO.class);
        List<Long> connectedEnvList = clusterConnectionHandler.getConnectedEnvList();
        List<Long> updatedEnvList = clusterConnectionHandler.getUpdatedEnvList();

        devopsClusterRepVO.setConnect(isConnect(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
        devopsClusterRepVO.setUpgrade(isUpdated(connectedEnvList, updatedEnvList, devopsClusterRepVO.getId()));
        if (devopsClusterRepVO.getUpgrade()) {
            devopsClusterRepVO.setUpgradeMessage("Version is too low, please upgrade!");
        }
        return devopsClusterRepVO;
    }


}
