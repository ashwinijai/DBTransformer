package com.db.util.dbTransformer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ColumnProperties {
    @NotBlank
    private String columnName;
    @NotBlank
    @Pattern(regexp = "VARCHAR|NUMBER|CLOB")
    private String dataType;
    @NotBlank
    private Long size;
    @NotBlank
    @Pattern(regexp = "true|false")
    private String isMandatory;
}
