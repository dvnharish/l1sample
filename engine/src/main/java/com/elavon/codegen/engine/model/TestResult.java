package com.elavon.codegen.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents test results for an operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {
    private boolean passed;
    private String message;
    private long duration;
}
