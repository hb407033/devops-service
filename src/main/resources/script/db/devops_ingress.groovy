package script.db

databaseChangeLog(logicalFilePath: 'dba/devops_ingress.groovy') {
    changeSet(author: 'Runge', id: '2018-04-19-create-table') {
        createTable(tableName: "devops_ingress", remarks: '域名管理') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'project_id', type: 'BIGINT UNSIGNED', remarks: '项目ID')
            column(name: 'env_id', type: 'BIGINT UNSIGNED', remarks: '环境 ID')
            column(name: 'name', type: 'VARCHAR(253)', remarks: '域名名称')
            column(name: 'domain', type: 'VARCHAR(253)', remarks: '域名地址')
            column(name: 'is_usable', type: 'TINYINT UNSIGNED', remarks: '是否可用', defaultValueBoolean: 'false')

            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }
}