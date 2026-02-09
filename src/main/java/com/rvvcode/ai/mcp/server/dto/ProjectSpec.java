package com.rvvcode.ai.mcp.server.dto;

import java.util.List;
import java.util.Map;

public record ProjectSpec(
                String projectName,
                String basePackage,
                String apiRequirements,
                String apiGoldStandards,
                Map<String, List<String>> packageStructure // Key: package name (relative to base), Value: List of class
                                                           // names
) {
}
