package com.ensambla.backend.infrastructure.presentation.dto.sistema;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Los datos opcionales que faltan no se envían: la pantalla los valida como "ausentes", no como nulos. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SistemaWebResponse(
        String id,
        String name,
        String createdAt,
        String updatedAt,
        List<SeccionWebResponse> sections,
        String template,
        OpcionesWebResponse options,
        String layout,
        boolean demoAlmacen) {

    public record OpcionesWebResponse(boolean movements, boolean expenses) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SeccionWebResponse(String id, String name, List<BloqueWebResponse> blocks) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BloqueWebResponse(
            String id,
            String type,
            String name,
            String description,
            String color,
            String width,
            List<CampoWebResponse> fields,
            List<Map<String, Object>> rows,
            String content,
            Boolean startRow,
            Integer stockDirection) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CampoWebResponse(
            String key,
            String label,
            String type,
            boolean required,
            List<String> options,
            BigDecimal min,
            String target) {
    }
}
