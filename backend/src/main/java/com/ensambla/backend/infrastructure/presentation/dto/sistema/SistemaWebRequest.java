package com.ensambla.backend.infrastructure.presentation.dto.sistema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** El sistema tal como lo manda la pantalla: se guarda entero y con los ids que ella le puso. */
public record SistemaWebRequest(
        @Size(max = 200) String id,
        @NotNull @Size(max = 500) String name,
        String createdAt,
        String updatedAt,
        @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid SeccionWebRequest> sections,
        @Size(max = 40) String template,
        @Valid OpcionesWebRequest options,
        @Size(max = 40) String layout,
        Boolean demoAlmacen) {

    public record OpcionesWebRequest(boolean movements, boolean expenses) {
    }

    public record SeccionWebRequest(
            @NotNull @Size(max = 200) String id,
            @NotNull @Size(max = 500) String name,
            @NotNull @Size(max = 100) List<@NotNull @Valid BloqueWebRequest> blocks) {
    }

    public record BloqueWebRequest(
            @NotNull @Size(max = 200) String id,
            @Size(max = 40) String type,
            @NotNull @Size(max = 500) String name,
            @Size(max = 5000) String description,
            @Size(max = 40) String color,
            @Size(max = 40) String width,
            @NotNull @Size(max = 100) List<@NotNull @Valid CampoWebRequest> fields,
            @NotNull @Size(max = 5000) List<@NotNull Map<String, Object>> rows,
            @Size(max = 20000) String content,
            Boolean startRow,
            Integer stockDirection) {
    }

    public record CampoWebRequest(
            @NotNull @Size(min = 1, max = 200) String key,
            @NotNull @Size(max = 500) String label,
            @Size(max = 40) String type,
            boolean required,
            @Size(max = 100) List<@NotNull @Size(max = 200) String> options,
            BigDecimal min,
            @Size(max = 200) String target) {
    }
}
