package com.ensambla.backend.infrastructure.ia;

import java.util.Map;

/** Una herramienta tal como se le describe al modelo ({@code ia/herramientas-taller.json}): nombre, uso y esquema. */
public record DefinicionHerramienta(String name, String description, Map<String, Object> inputSchema) {
}
