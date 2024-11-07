package com.db.util.dbTransformer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformRequest {
    private DBProperties sourceDb;
    private DBProperties targetDb;
    private List<DBMapping> dbMapping;
    private List<ColumnProperties> sourceProperties;
    private List<ColumnProperties> targetProperties;
}
