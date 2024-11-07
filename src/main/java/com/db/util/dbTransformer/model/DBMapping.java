package com.db.util.dbTransformer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBMapping {
    private String sourceColumn;
    private String targetColumn;
}
