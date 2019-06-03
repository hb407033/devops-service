import React, { Component, Fragment } from 'react';
import { observer, inject } from 'mobx-react';
import { injectIntl, FormattedMessage } from 'react-intl';
import { Content } from '@choerodon/boot';
import _ from 'lodash';
import {
  Form,
  Input,
  Modal,
  Radio,
  Table,
  Tag,
  Button,
} from 'choerodon-ui';
import CertConfig from '../../../../components/certConfig';
import Tips from '../../../../components/Tips/Tips';
import InterceptMask from '../../../../components/interceptMask/InterceptMask';
import { HEIGHT } from '../../../../common/Constants';
import { handleCheckerProptError } from '../../../../utils';

import '../../../main.scss';
import './CertificateCreate.scss';
import '../../../project/envPipeline/EnvPipeLineHome.scss';

const { Sidebar } = Modal;
const { Item: FormItem } = Form;
const { Group: RadioGroup } = Radio;
const { TextArea } = Input;
const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 100 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 26 },
  },
};

@Form.create({})
@injectIntl
@inject('AppState')
@observer
export default class CertificateCreate extends Component {
  state = {
    submitting: false,
    type: 'request',
    keyLoad: false,
    certLoad: false,
    checked: true,
    createSelectedRowKeys: [],
    createSelected: [],
    selected: [],
    createSelectedTemp: [],
    uploadMode: false,
  };

  /**
   * 校验证书名称唯一性
   */
  checkName = _.debounce((rule, value, callback) => {
    const {
      store,
      intl: { formatMessage },
      AppState: { currentMenuType: { organizationId } },
    } = this.props;
    store.checkCertName(organizationId, value)
      .then(data => {
        if (data && data.failed) {
          callback(formatMessage({ id: 'ctf.name.check.exist' }));
        } else {
          callback();
        }
      })
      .catch(() => callback());
  }, 1000);

  async componentDidMount() {
    const {
      showType,
      store,
      id,
      AppState: { currentMenuType: { organizationId } },
    } = this.props;

    if (showType === 'edit') {
      store.loadPro(organizationId, id);
      store.loadTagKeys(organizationId, id);
      const response = await store.loadCertById(organizationId, id)
        .catch(e => {
          Choerodon.handleResponseError(e);
        });

      if (handleCheckerProptError(response)) {
        this.setState({
          checked: data.skipCheckProjectPermission,
        });
      }
    } else {
      store.loadPro(organizationId, null);
    }
  }

  handleSubmit = async e => {
    e.preventDefault();
    const {
      form,
      store,
      showType,
      AppState: { currentMenuType: { organizationId } },
    } = this.props;

    const {
      checked,
      createSelectedRowKeys,
    } = this.state;

    this.setState({ submitting: true });

    let p = null;
    const submitFunc = {
      create: data => {
        const _data = {
          ...data,
          skipCheckProjectPermission: checked,
          projects: createSelectedRowKeys,
        };

        p = store.createCert(organizationId, _data);
      },
      edit: (data) => {
        const { getCert, getTagKeys } = store;
        const proIds = _.map(getTagKeys, t => t.id);

        const _data = {
          ...data,
          skipCheckProjectPermission: checked,
          projects: proIds,
        };

        p = store.updateCert(organizationId, getCert.id, _data);
      },
    };

    form.validateFieldsAndScroll((err, data) => {
      if (!err) {
        submitFunc[showType](data);
        this.handleResponse(p);
      } else {
        this.setState({ submitting: false });
      }
    });

  };

  /**
   * 处理创建证书请求所返回的数据
   * @param promise
   */
  handleResponse = async promise => {
    if (!promise) return;

    const {
      store,
      AppState: { currentMenuType: { organizationId } },
    } = this.props;

    const result = await promise.catch(error => {
      Choerodon.handleResponseError(error);
      this.setState({ submitting: false });
    });

    this.setState({ submitting: false });

    if (handleCheckerProptError(result)) {
      const initSize = HEIGHT <= 900 ? 10 : 15;
      const filter = {
        page: 0,
        pageSize: initSize,
        param: [],
        filters: {},
        postData: { searchParam: {}, param: '' },
        sorter: {
          field: 'id',
          columnKey: 'id',
          order: 'descend',
        },
      };
      store.setTableFilter(filter);
      store.loadCertData(
        organizationId,
        0,
        initSize,
        { field: 'id', order: 'descend' },
        { searchParam: {}, param: '' },
      );
      this.handleClose();
    }
  };

  /**
   * 关闭弹框
   */
  handleClose = () => {
    const { onClose, store } = this.props;
    store.setInfo({
      filters: {},
      sort: { columnKey: 'id', order: 'descend' },
      paras: [],
    });
    store.setProPageInfo({
      number: 0,
      size: HEIGHT <= 900 ? 10 : 15,
      totalElements: 0,
    });
    onClose();
  };

  /**
   * 域名格式检查
   * @param rule
   * @param value
   * @param callback
   */
  checkDomain = (rule, value, callback) => {
    const { intl: { formatMessage } } = this.props;
    const p = /^([a-z0-9]([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)+)$/;
    if (p.test(value)) {
      callback();
    } else {
      callback(formatMessage({ id: 'ctf.domain.check.failed' }));
    }
  };

  onCreateSelectChange = (keys, selected) => {
    let s = [];
    const a = this.state.createSelectedTemp.concat(selected);
    this.setState({ createSelectedTemp: a });
    _.map(keys, o => {
      if (_.filter(a, ['id', o]).length) {
        s.push(_.filter(a, ['id', o])[0]);
      }
    });
    this.setState({
      createSelectedRowKeys: keys,
      createSelected: s,
    });
  };

  /**
   * 分配权限
   * @param keys
   * @param selected
   */
  onSelectChange = (keys, selected) => {
    const { store } = this.props;
    const {
      getTagKeys: tagKeys,
    } = store;
    let s = [];
    const a = tagKeys.length ? tagKeys.concat(selected) : this.state.selected.concat(selected);
    this.setState({ selected: a });
    _.map(keys, o => {
      if (_.filter(a, ['id', o]).length) {
        s.push(_.filter(a, ['id', o])[0]);
      }
    });
    store.setTagKeys(s);
  };

  cbChange = (e) => {
    this.setState({ checked: e.target.value });
  };

  /**
   * table 操作
   * @param pagination
   * @param filters
   * @param sorter
   * @param paras
   */
  tableChange = (pagination, filters, sorter, paras) => {
    const {
      store,
      showType,
      AppState: { currentMenuType: { organizationId } },
    } = this.props;
    store.setInfo({ filters, sort: sorter, paras });
    let sort = { field: '', order: 'desc' };
    if (sorter.column) {
      sort.field = sorter.field || sorter.columnKey;
      if (sorter.order === 'ascend') {
        sort.order = 'asc';
      } else if (sorter.order === 'descend') {
        sort.order = 'desc';
      }
    }
    let page = pagination.current - 1;
    const postData = [paras.toString()];
    if (showType === 'create') {
      store.loadPro(organizationId, null, page, pagination.pageSize, sort, postData);
    } else {
      const id = store.getCert ? store.getCert.id : null;
      store.loadPro(organizationId, id, page, pagination.pageSize, sort, postData);
    }
  };

  changeUploadMode = () => {
    this.setState({ uploadMode: !this.state.uploadMode });
  };

  render() {
    const {
      showType,
      form: { getFieldDecorator },
      intl: { formatMessage },
      AppState: { currentMenuType: { name } },
      store: {
        getCert,
        getInfo: { paras },
        getProPageInfo,
        getProData: proData,
        getTagKeys: tagKeys,
        getTableLoading: tableLoading,
      },
    } = this.props;

    const {
      submitting,
      checked,
      createSelectedRowKeys,
      createSelected,
      uploadMode,
    } = this.state;

    const rowCreateSelection = {
      selectedRowKeys: createSelectedRowKeys,
      onChange: this.onCreateSelectChange,
    };
    const rowSelection = {
      selectedRowKeys: _.map(tagKeys, s => s.id),
      onChange: this.onSelectChange,
    };

    const columns = [{
      key: 'name',
      title: formatMessage({ id: 'cluster.project.name' }),
      dataIndex: 'name',
    }, {
      key: 'code',
      title: formatMessage({ id: 'cluster.project.code' }),
      dataIndex: 'code',
    }];

    const tagCreateDom = _.map(createSelected, ({ id, name, code }) =>
      <Tag className="c7n-env-tag" key={id}>
        {name} {code}
      </Tag>,
    );

    const tagDom = _.map(tagKeys, (t) => t
      ? <Tag className="c7n-env-tag" key={t.id}>
        {t.name} {t.code}
      </Tag>
      : null,
    );

    return (
      <div className="c7n-region">
        <Sidebar
          destroyOnClose
          cancelText={<FormattedMessage id="cancel" />}
          okText={<FormattedMessage id={`${showType === 'create' ? showType : 'save'}`} />}
          title={<FormattedMessage id={`ctf.sidebar.${showType}`} />}
          visible={showType !== ''}
          onOk={this.handleSubmit}
          onCancel={this.handleClose}
          confirmLoading={submitting}
        >
          <Content
            code={`certificate.${showType}`}
            values={{ name: getCert ? getCert.name : name }}
            className="c7n-ctf-create sidebar-content"
          >
            <Form layout="vertical">
              <FormItem className="c7n-select_512" {...formItemLayout}>
                {getFieldDecorator('name', {
                  rules: [
                    {
                      required: true,
                      message: formatMessage({ id: 'required' }),
                    },
                    {
                      validator: showType === 'create' ? this.checkName : null,
                    },
                  ],
                  initialValue: getCert ? getCert.name : '',
                })(
                  <Input
                    maxLength={40}
                    type="text"
                    label={<FormattedMessage id="ctf.name" />}
                    disabled={showType === 'edit'}
                  />,
                )}
              </FormItem>
              {showType === 'create' && <div className="c7n-creation-ctf-title">
                <FormattedMessage id="ctf.upload" />
              </div>}
              <div className={showType === 'create' ? 'c7n-creation-panel' : ''}>
                <FormItem
                  className={`c7n-select_${showType === 'create' ? '480' : '512'}`}
                  {...formItemLayout}
                >
                  {getFieldDecorator('domain', {
                    rules: [
                      {
                        required: true,
                        message: formatMessage({ id: 'required' }),
                      },
                      {
                        validator: showType === 'create' ? this.checkDomain : null,
                      },
                    ],
                    initialValue: getCert ? getCert.domain : '',
                  })(
                    <Input
                      type="text"
                      maxLength={50}
                      label={<FormattedMessage id="ctf.config.domain" />}
                      disabled={showType === 'edit'}
                    />,
                  )}
                </FormItem>
                {showType === 'create' && <Fragment>
                  <div className="c7n-creation-add-title">
                    <Tips type="title" data="certificate.file.add" />
                    <Button
                      type="primary"
                      funcType="flat"
                      onClick={this.changeUploadMode}
                    >
                      <FormattedMessage id="ctf.upload.mode" />
                    </Button>
                  </div>
                  <CertConfig
                    isUploadMode={uploadMode}
                  />
                </Fragment>}
              </div>
              <div className="c7n-creation-ctf-title">
                <Tips type="title" data="certificate.permission" />
              </div>
              <div className="c7n-certificate-radio">
                <RadioGroup
                  label={<FormattedMessage id="certificate.public" />}
                  onChange={this.cbChange}
                  value={checked}
                >
                  <Radio value={true}><FormattedMessage id="cluster.project.all" /></Radio>
                  <Radio value={false}><FormattedMessage id="cluster.project.part" /></Radio>
                </RadioGroup>
              </div>
              {checked ? null : <div>
                <div className="c7n-sidebar-form">
                  <Table
                    rowSelection={showType === 'create' ? rowCreateSelection : rowSelection}
                    columns={columns}
                    dataSource={proData}
                    filterBarPlaceholder={formatMessage({ id: 'filter' })}
                    pagination={getProPageInfo}
                    loading={tableLoading}
                    onChange={this.tableChange}
                    rowKey={record => record.id}
                    filters={paras.slice()}
                  />
                </div>
                <div className="c7n-env-tag-title">
                  <FormattedMessage id="cluster.authority.project" />
                </div>
                <div className="c7n-env-tag-wrap">
                  {showType === 'create' ? tagCreateDom : tagDom}
                </div>
              </div>}
            </Form>
            <InterceptMask visible={submitting} />
          </Content>
        </Sidebar>
      </div>
    );
  }
}
