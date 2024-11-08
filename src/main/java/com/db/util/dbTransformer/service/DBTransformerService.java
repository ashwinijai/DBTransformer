package com.db.util.dbTransformer.service;

import com.db.util.dbTransformer.model.ColumnProperties;
import com.db.util.dbTransformer.model.DBMapping;
import com.db.util.dbTransformer.model.DBProperties;
import com.db.util.dbTransformer.model.TransformRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS;
import static com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED;

@Service
@Slf4j
public class DBTransformerService {

    public String getJsonSchema() throws JsonProcessingException {
        JakartaValidationModule module = new JakartaValidationModule(NOT_NULLABLE_FIELD_IS_REQUIRED, INCLUDE_PATTERN_EXPRESSIONS);
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON).with(module);
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        JsonNode jsonSchema = generator.generateSchema(TransformRequest.class);
        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(jsonSchema.toString(), Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    public Set<ValidationMessage> validateJson(JsonNode jsonNode) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        Resource resource = new ClassPathResource("/schema.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()), 1024);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            stringBuilder.append(line).append('\n');
        }
        br.close();
        String schemaJson = stringBuilder.toString();
        JsonSchema jsonSchema = factory.getSchema(schemaJson);
        return jsonSchema.validate(jsonNode);

    }

    public List<String> transform(TransformRequest transformRequest) {
        DBProperties sourceDbProps = transformRequest.getSourceDb();
        DBProperties targetDbProps = transformRequest.getTargetDb();
        List<ColumnProperties> sourceColumnProps = transformRequest.getSourceProperties();
        List<ColumnProperties> targetColumnProps = transformRequest.getTargetProperties();
        List<DBMapping> dbMappingList = transformRequest.getDbMapping();
        Map<String, String> dbMappingMap = dbMappingList.stream().collect(Collectors.toMap(DBMapping::getTargetColumn, DBMapping::getSourceColumn));
        Set<String> validationMessages = validateTableStructures(sourceColumnProps, targetColumnProps, dbMappingMap);
        if (validationMessages.isEmpty()) {
            List<Map<String, Object>> outputRows = executeSelect(sourceDbProps, sourceColumnProps);
            return executeInsert(targetDbProps, targetColumnProps, dbMappingMap, outputRows);
        } else {
            return validationMessages.stream().toList();
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
                    if (sourceColumn.getSize() > targetColumn.getSize()) {
                        validationSet.add("Size doesn't match for Target Column - " + targetColumn.getColumnName() + " and Source Column - " + sourceColumn.getColumnName());
                    }
                    if (targetColumn.getIsMandatory().equals("true") && sourceColumn.getIsMandatory().equals("false")) {
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
        String tableNameString = sourceDbProps.getSchemaName() + "." + sourceDbProps.getTableName();
        String whereString = (null == sourceDbProps.getWhereClause()) ? "" : "WHERE " + sourceDbProps.getWhereClause();
        String selectQuery = "select " + columnNamesString + " from " + tableNameString + whereString;
        log.info("SELECT QUERY TO BE EXECUTED - {}",selectQuery);
        List<Map<String, Object>> resultRows = sourceJdbc.queryForList(selectQuery);
        log.info("RESULT SIZE - {}", resultRows.size());
        return resultRows;
    }

    public JdbcTemplate getJdbcTemplate(DBProperties dbProperties) {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        String driverName = dbProperties.getDbType().equals("H2") ? "org.h2.Driver" : "oracle.jdbc.driver.OracleDriver";
        dataSourceBuilder.driverClassName(driverName);
        dataSourceBuilder.url(dbProperties.getUrl());
        dataSourceBuilder.username(dbProperties.getUserName());
        dataSourceBuilder.password(dbProperties.getPassword());
        return new JdbcTemplate(dataSourceBuilder.build());
    }

    public List<String> executeInsert(DBProperties targetDbProps, List<ColumnProperties> targetColumnProps, Map<String, String> dbMappingMap, List<Map<String, Object>> sourceOutput) {
        List<String> resultList = new ArrayList<>();
        JdbcTemplate targetJdbc = getJdbcTemplate(targetDbProps);
        List<String> targetColumnNames = targetColumnProps.stream().map(ColumnProperties::getColumnName).toList();
        String columnNamesString = String.join(",", targetColumnNames);
        sourceOutput.forEach(s -> {
            StringBuilder valueStringBuilder = new StringBuilder();
            targetColumnNames.forEach(t -> {
                valueStringBuilder.append("'").append(s.get(t)).append("',");
            });
            String valueString = StringUtils.removeEnd(valueStringBuilder.toString(), ",");
            String query = "INSERT INTO "+targetDbProps.getSchemaName()+"."+targetDbProps.getTableName()+"("+columnNamesString+") VALUES ("+valueString+")";
            log.info("INSERT QUERY TO BE EXECUTED - {}",query);
            try {
                int result = targetJdbc.update(query);
                if (result > 0) {
                    resultList.add("Inserted Successfully - " + query);
                    log.info("Inserted Successfully - {}" , query);
                }
            }
            catch(Exception e){
                resultList.add("Inserted Unsuccessful - " + query + e.getMessage());
                log.info("Inserted Unsuccessful - {}" , query);
            }
        });
        return resultList;
        }
    }
