package com.db.util.dbTransformer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBProperties {
    @NotBlank
    @Pattern(regexp = "ORACLE|H2")
    private String dbType;
    @NotBlank
    private String url;
    @NotBlank
    private String userName;
    @NotBlank
    private String password;
    @NotBlank
    private String tableName;
    @NotBlank
    private String schemaName;
    private String whereClause;
}
