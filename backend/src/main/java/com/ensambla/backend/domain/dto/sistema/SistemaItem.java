package com.ensambla.backend.domain.dto.sistema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** El sistema con la forma en que lo arma y lo muestra la pantalla; los nombres de los datos son los del front. */
public record SistemaItem(
        String id,
        String name,
        String createdAt,
        String updatedAt,
        List<SeccionItem> sections,
        String template,
        OpcionesItem options,
        String layout,
        boolean demoAlmacen) {

    public record OpcionesItem(boolean movements, boolean expenses) {
    }

    public record SeccionItem(String id, String name, List<BloqueItem> blocks) {
    }

    /** {@code rows}: cada fila trae su {@code id} y los valores por clave de columna. */
    public record BloqueItem(
            String id,
            String type,
            String name,
            String description,
            String color,
            String width,
            List<CampoItem> fields,
            List<Map<String, Object>> rows,
            String content,
            Boolean startRow,
            Integer stockDirection) {
    }

    public record CampoItem(
            String key,
            String label,
            String type,
            boolean required,
            List<String> options,
            BigDecimal min,
            String target) {
    }
}
