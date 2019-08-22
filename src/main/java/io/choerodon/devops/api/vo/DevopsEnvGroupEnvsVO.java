package io.choerodon.devops.api.vo;

import java.util.List;

public class DevopsEnvGroupEnvsVO {
    private Long devopsEnvGroupId;
    private String devopsEnvGroupName;
    private List<DevopsEnviromentRepVO> devopsEnviromentRepDTOs;
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getDevopsEnvGroupId() {
        return devopsEnvGroupId;
    }

    public void setDevopsEnvGroupId(Long devopsEnvGroupId) {
        this.devopsEnvGroupId = devopsEnvGroupId;
    }

    public String getDevopsEnvGroupName() {
        return devopsEnvGroupName;
    }

    public void setDevopsEnvGroupName(String devopsEnvGroupName) {
        this.devopsEnvGroupName = devopsEnvGroupName;
    }

    public List<DevopsEnviromentRepVO> getDevopsEnviromentRepDTOs() {
        return devopsEnviromentRepDTOs;
    }

    public void setDevopsEnviromentRepDTOs(List<DevopsEnviromentRepVO> devopsEnviromentRepDTOs) {
        this.devopsEnviromentRepDTOs = devopsEnviromentRepDTOs;
    }
}
