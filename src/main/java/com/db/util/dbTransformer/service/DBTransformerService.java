package com.db.util.dbTransformer.service;

import com.db.util.dbTransformer.model.ColumnProperties;
import com.db.util.dbTransformer.model.DBMapping;
import com.db.util.dbTransformer.model.DBProperties;
import com.db.util.dbTransformer.model.TransformRequest;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DBTransformerService {

    public String transform(TransformRequest transformRequest) {
        DBProperties sourceDbProps = transformRequest.getSourceDb();
        DBProperties targetDbProps = transformRequest.getTargetDb();
        List<ColumnProperties> sourceColumnProps = transformRequest.getSourceProperties();
        List<ColumnProperties> targetColumnProps = transformRequest.getTargetProperties();
        List<DBMapping> dbMappingList = transformRequest.getDbMapping();
        Map<String, String> dbMappingMap = dbMappingList.stream().collect(Collectors.toMap(DBMapping::getTargetColumn, DBMapping::getSourceColumn));
        Set<String> validationMessages = validateTableStructures(sourceColumnProps, targetColumnProps, dbMappingMap);
        if (validationMessages.isEmpty()) {
            List<Map<String,Object>> rows = executeSelect(sourceDbProps, sourceColumnProps);
            return rows.toString();
        } else {
            return validationMessages.toString();
        }
    }

    private Set<String> validateTableStructures(List<ColumnProperties> sourceColumnProps, List<ColumnProperties> targetColumnProps, Map<String, String> dbMappingMap) {
        Set<String> validationSet = new HashSet<>();
        boolean isDataTypeMatching = false;
        for (ColumnProperties targetColumn : targetColumnProps) {
            String sourceColumnName = dbMappingMap.get(targetColumn.getColumnName());
            for (ColumnProperties sourceColumn : sourceColumnProps) {
                if (sourceColumn.getColumnName().equals(sourceColumnName)) {
                    if (targetColumn.getDataType().equals("VARCHAR") && !(sourceColumn.getDataType().equals("NUMBER") || sourceColumn.getDataType().equals("VARCHAR"))) {
                        validationSet.add("Data Type doesn't match for Target Column - " + targetColumn.getColumnName() + " and Source Column - " + sourceColumn.getColumnName());
                    } else if (!targetColumn.getDataType().equals(sourceColumn.getDataType())) {
                        validationSet.add("Data Type doesn't match for Target Column - " + targetColumn.getColumnName() + " and Source Column - " + sourceColumn.getColumnName());
                    }
                    if (!targetColumn.getSize().equals(sourceColumn.getSize())) {
                        validationSet.add("Size doesn't match for Target Column - " + targetColumn.getColumnName() + " and Source Column - " + sourceColumn.getColumnName());
                    }
                    if (!targetColumn.getIsMandatory().equals(sourceColumn.getIsMandatory()) ){
                        validationSet.add("Not Null constraint doesn't match for Target Column - " + targetColumn.getColumnName() + " and Source Column - " + sourceColumn.getColumnName());
                    }
                }
            }
        }
        return validationSet;
    }

    public List<Map<String, Object>> executeSelect(DBProperties sourceDbProps, List<ColumnProperties> sourceColumnProps) {
        JdbcTemplate sourceJdbc = getJdbcTemplate(sourceDbProps);
        List<String> sourceColumnNames = sourceColumnProps.stream().map(ColumnProperties::getColumnName).toList();
        String columnNamesString = String.join(",", sourceColumnNames);
        String tableNameString  = (null==sourceDbProps.getSchemaName())?sourceDbProps.getTableName():sourceDbProps.getSchemaName()+"."+sourceDbProps.getTableName();
        String whereString = (null==sourceDbProps.getWhereClause())?"":"WHERE "+sourceDbProps.getWhereClause();
        return sourceJdbc.queryForList("select " + columnNamesString + " from " + tableNameString+whereString);
    }

    public JdbcTemplate getJdbcTemplate(DBProperties dbProperties) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.h2.Driver");
        dataSourceBuilder.url("jdbc:h2:file:/Users/ashwinijayaraman/Downloads/Practice/DB;AUTO_SERVER=TRUE");
        dataSourceBuilder.username(dbProperties.getUserName());
        dataSourceBuilder.password(dbProperties.getPassword());
        return new JdbcTemplate(dataSourceBuilder.build());
    }
}
