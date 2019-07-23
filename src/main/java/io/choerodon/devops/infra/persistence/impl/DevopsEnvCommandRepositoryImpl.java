package io.choerodon.devops.infra.persistence.impl;

import java.util.Date;
import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.repository.DevopsCommandEventRepository;
import io.choerodon.devops.domain.application.repository.DevopsEnvCommandLogRepository;
import io.choerodon.devops.domain.application.repository.DevopsEnvCommandRepository;
import io.choerodon.devops.domain.application.repository.DevopsEnvCommandValueRepository;
import io.choerodon.devops.infra.util.PageRequestUtil;
import io.choerodon.devops.infra.dto.ApplicationInstanceDTO;
import io.choerodon.devops.infra.dto.DevopsEnvCommandDTO;
import io.choerodon.devops.infra.mapper.DevopsEnvCommandMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author crcokitwood
 */
@Service
public class DevopsEnvCommandRepositoryImpl implements DevopsEnvCommandRepository {
    private static final String INSTANCE_TYPE = "instance";

    @Autowired
    DevopsEnvCommandValueRepository devopsEnvCommandValueRepository;
    @Autowired
    DevopsEnvCommandLogRepository devopsEnvCommandLogRepository;
    @Autowired
    DevopsCommandEventRepository devopsCommandEventRepository;
    @Autowired
    private DevopsEnvCommandMapper devopsEnvCommandMapper;


    @Override
    public DevopsEnvCommandVO baseCreate(DevopsEnvCommandVO devopsEnvCommandE) {
        DevopsEnvCommandDTO devopsEnvCommandDO = ConvertHelper.convert(devopsEnvCommandE, DevopsEnvCommandDTO.class);
        if (devopsEnvCommandMapper.insert(devopsEnvCommandDO) != 1) {
            throw new CommonException("error.env.command.insert");
        }
        return ConvertHelper.convert(devopsEnvCommandDO, DevopsEnvCommandVO.class);
    }

    @Override
    public DevopsEnvCommandVO baseQueryByObject(String objectType, Long objectId) {
        return ConvertHelper.convert(
                devopsEnvCommandMapper.queryByObject(objectType, objectId), DevopsEnvCommandVO.class);
    }

    @Override
    public DevopsEnvCommandVO baseUpdate(DevopsEnvCommandVO devopsEnvCommandE) {
        DevopsEnvCommandDTO devopsEnvCommandDO = ConvertHelper.convert(devopsEnvCommandE, DevopsEnvCommandDTO.class);
        DevopsEnvCommandDTO newDevopsEnvCommandDO = devopsEnvCommandMapper
                .selectByPrimaryKey(devopsEnvCommandDO.getId());
        devopsEnvCommandDO.setObjectVersionNumber(newDevopsEnvCommandDO.getObjectVersionNumber());
        if (devopsEnvCommandMapper.updateByPrimaryKeySelective(devopsEnvCommandDO) != 1) {
            throw new CommonException("error.env.command.update");
        }
        return ConvertHelper.convert(devopsEnvCommandDO, DevopsEnvCommandVO.class);
    }

    @Override
    public DevopsEnvCommandVO baseQuery(Long id) {
        DevopsEnvCommandDTO devopsEnvCommandDO = devopsEnvCommandMapper.selectByPrimaryKey(id);
        return ConvertHelper.convert(devopsEnvCommandDO, DevopsEnvCommandVO.class);
    }

    @Override
    public List<DevopsEnvCommandVO> baseListByEnvId(Long envId) {
        DevopsEnvCommandDTO devopsEnvCommandDO = new DevopsEnvCommandDTO();
        return ConvertHelper.convertList(devopsEnvCommandMapper.select(devopsEnvCommandDO), DevopsEnvCommandVO.class);
    }

    @Override
    public List<DevopsEnvCommandVO> baseListInstanceCommand(String objectType, Long objectId) {
        return ConvertHelper.convertList(devopsEnvCommandMapper.listInstanceCommand(objectType, objectId), DevopsEnvCommandVO.class);
    }

    @Override
    public PageInfo<DevopsEnvCommandVO> basePageByObject(PageRequest pageRequest, String objectType, Long objectId, Date startTime, Date endTime) {
        PageInfo<ApplicationInstanceDTO> applicationInstanceDOPage = PageHelper.startPage(pageRequest.getPage(),pageRequest.getSize(), PageRequestUtil.getOrderBy(pageRequest)).doSelectPageInfo(() ->
                devopsEnvCommandMapper.listByObject(objectType, objectId, startTime == null ? null : new java.sql.Date(startTime.getTime()), endTime == null ? null : new java.sql.Date(endTime.getTime())));
        return ConvertPageHelper.convertPageInfo(applicationInstanceDOPage, DevopsEnvCommandVO.class);
    }

    @Override
    public void baseDelete(Long commandId) {
        DevopsEnvCommandDTO devopsEnvCommandDO = new DevopsEnvCommandDTO();
        devopsEnvCommandDO.setId(commandId);
        devopsEnvCommandMapper.deleteByPrimaryKey(devopsEnvCommandDO);
    }

    @Override
    public List<DevopsEnvCommandVO> baseListByObject(String objectType, Long objectId) {
        DevopsEnvCommandDTO devopsEnvCommandDO = new DevopsEnvCommandDTO();
        devopsEnvCommandDO.setObjectId(objectId);
        devopsEnvCommandDO.setObject(objectType);
        return ConvertHelper.convertList(devopsEnvCommandMapper.select(devopsEnvCommandDO), DevopsEnvCommandVO.class);
    }

    @Override
    public void baseDeleteByEnvCommandId(DevopsEnvCommandVO commandE) {
        if (commandE.getDevopsEnvCommandValueDTO() != null) {
            devopsEnvCommandValueRepository.baseDeleteById(commandE.getDevopsEnvCommandValueDTO().getId());
        }
        devopsEnvCommandLogRepository.baseDeleteByCommandId(commandE.getId());
        devopsCommandEventRepository.baseDeleteByCommandId(commandE.getId());
        devopsEnvCommandMapper.deleteByPrimaryKey(commandE.getId());
    }
}
