package io.choerodon.devops.app.service;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.*;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by younger on 2018/3/28.
 */
public interface ApplicationService {

    /**
     * 项目下创建应用
     *
     * @param applicationDTO 应用信息
     * @return ApplicationTemplateDTO
     */
    ApplicationRepDTO create(Long projectId, ApplicationDTO applicationDTO);


    /**
     * 项目下查询单个应用信息
     *
     * @param projectId     项目id
     * @param applicationId 应用Id
     * @return ApplicationRepDTO
     */
    ApplicationRepDTO query(Long projectId, Long applicationId);

    /**
     * 项目下更新应用信息
     *
     * @param projectId            项目id
     * @param applicationUpdateDTO 应用信息
     * @return Boolean
     */
    Boolean update(Long projectId, ApplicationUpdateDTO applicationUpdateDTO);


    /**
     * 项目下启用停用应用信息
     *
     * @param applicationId 应用id
     * @param active        启用停用
     * @return Boolean
     */
    Boolean active(Long applicationId, Boolean active);

    /**
     * 组织下分页查询应用
     *
     * @param pageRequest 分页参数
     * @param params      参数
     * @return Page
     */
    Page<ApplicationRepDTO> listByOptions(Long projectId,
                                          PageRequest pageRequest,
                                          String params);

    /**
     * 处理应用创建逻辑
     *
     * @param gitlabProjectEventDTO 应用信息
     */
    void operationApplication(GitlabProjectEventDTO gitlabProjectEventDTO);

    Boolean applicationExist(String uuid);

    /**
     * 项目下应用查询ci脚本文件
     *
     * @param token token
     * @param type  类型
     * @return File
     */
    String queryFile(String token, String type);

    /**
     * 根据环境id获取已部署正在运行实例的应用
     *
     * @param projectId 项目id
     * @return list of ApplicationRepDTO
     */
    List<ApplicationCodeDTO> listByEnvId(Long projectId, Long envId, String status);

    /**
     * 项目下查询所有已经启用的应用
     *
     * @param projectId 项目id
     * @return list of ApplicationRepDTO
     */
    List<ApplicationRepDTO> listByActive(Long projectId);

    /**
     * 创建应用校验名称是否存在
     *
     * @param projectId 项目id
     * @param name      应用name
     * @return
     */
    void checkName(Long projectId, String name);

    /**
     * 创建应用校验编码是否存在
     *
     * @param projectId 项目ID
     * @param code      应用code
     * @return
     */
    void checkCode(Long projectId, String code);

    /**
     * 查询应用模板
     *
     * @param projectId 项目ID
     * @return Page
     */
    List<ApplicationTemplateRepDTO> listTemplate(Long projectId);

    /**
     * 项目下查询已经启用有版本未发布的应用
     *
     * @param projectId 项目id
     * @return list of ApplicationRepDTO
     */
    List<ApplicationDTO> listByActiveAndPubAndVersion(Long projectId);
}
