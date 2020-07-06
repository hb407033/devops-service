package script.db.groovy.devops_service

databaseChangeLog(logicalFilePath: 'dba/devops_cd_stage_record.groovy') {
    changeSet(author: 'wanghao', id: '2020-07-02-create-table') {
        createTable(tableName: "devops_cd_stage_record", remarks: '阶段') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'pipeline_record_id', type: 'BIGINT UNSIGNED', remarks: '流水线记录Id')
            column(name: 'stage_id', type: 'BIGINT UNSIGNED', remarks: '阶段Id')
            column(name: 'stage_name', type: 'VARCHAR(50)', remarks: '阶段名称')
            column(name: 'status', type: 'VARCHAR(20)', remarks: '状态')
            column(name: 'trigger_type', type: 'VARCHAR(10)', remarks: '触发方式')
            column(name: 'execution_time', type: 'VARCHAR(255)', remarks: '执行时间')
            column(name: 'project_id', type: 'BIGINT UNSIGNED', remarks: '项目Id')

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }

    changeSet(author: 'wanghao', id: '2020-07-02-idx-pipeline-record-id') {
        createIndex(indexName: "idx_pipeline_record_id ", tableName: "devops_cd_stage_record") {
            column(name: "pipeline_record_id")
        }
    }
    changeSet(author: 'wanghao', id: '2020-07-05-add-column') {
        addColumn(tableName: 'devops_cd_stage_record') {
            column(name: 'sequence', type: 'BIGINT UNSIGNED', remarks: '阶段顺序')
        }
    }
    changeSet(author: 'wanghao', id: '2020-07-07-add-column') {
        addColumn(tableName: 'devops_cd_stage_record') {
            column(name: "started_date", type: "DATETIME", remarks: 'stage开始执行时间')
            column(name: "finished_date", type: "DATETIME", remarks: 'stage结束时间')
        }
    }
}