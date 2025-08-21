package com.elavon.codegen.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents file changes made during code generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChanges {
    private List<String> created;
    private List<String> updated;
    private List<String> deleted;
}
