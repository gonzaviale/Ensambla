package com.ensambla.backend.infrastructure.presentation.dto.importacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Parte JSON de {@code POST /api/importaciones/lectura}: el bloque al que irían los datos de la foto. Los tipos de
 * campo son los de la pantalla ({@code text}, {@code number}, {@code select}...).
 *
 * @param destinoExistente {@code true} si el bloque ya está en el sistema; sólo entonces se proponen las columnas que le faltan
 */
public record PedidoLecturaRequest(
        @NotBlank(message = "El bloque necesita un nombre")
        @Size(max = 120, message = "Máximo 120 caracteres")
        String nombre,
        @NotEmpty(message = "El bloque necesita al menos un dato")
        @Valid
        List<CampoPedido> campos,
        boolean destinoExistente
) {

    public record CampoPedido(
            @NotBlank(message = "El dato necesita una clave")
            String key,
            @NotBlank(message = "El dato necesita un nombre")
            String label,
            @NotBlank(message = "El dato necesita un tipo")
            String type,
            List<String> options
    ) {
    }
}
