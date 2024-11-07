package com.db.util.dbTransformer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ColumnProperties {
    private String columnName;
    private String dataType;
    private Long size;
    private String isMandatory;
}
