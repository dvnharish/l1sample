package com.elavon.codegen.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a mapping between legacy and target operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationMapping {
    private String legacy;
    private String target;
    private String tag;
}
