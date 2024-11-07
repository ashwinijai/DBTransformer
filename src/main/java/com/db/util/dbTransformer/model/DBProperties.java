package com.db.util.dbTransformer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBProperties {
    private String dbType;
    private String url;
    private String userName;
    private String password;
    private String tableName;
    private String schemaName;
    private String whereClause;
}
