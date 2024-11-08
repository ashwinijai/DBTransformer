package com.db.util.dbTransformer.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBMapping {
    @NotBlank
    private String sourceColumn;
    @NotBlank
    private String targetColumn;
}
