package com.ensambla.backend.infrastructure.presentation.dto.sami;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Un turno de la charla con Sami en el taller.
 *
 * @param mensaje   lo que la persona escribió o eligió en la botonera
 * @param estado    el sistema que se está armando, resumido (con las primeras filas de cada pieza)
 * @param historial los últimos mensajes de la charla, del más viejo al más nuevo
 * @param imagen    una foto opcional (cuaderno, lista, planilla) para que Sami arme las piezas y cargue lo que lee
 */
public record ConversarTallerRequest(
        @NotBlank @Size(max = 2500) String mensaje,
        @NotNull @Valid Estado estado,
        @Size(max = 10) List<@Valid Mensaje> historial,
        Imagen imagen
) {

    /**
     * La foto en base64 (también se acepta la data URL que da el navegador). El tipo y el tamaño (hasta 5 MB
     * decodificada) los valida el dominio, que responde 400 con un mensaje para la persona.
     *
     * @param tipo  {@code image/jpeg}, {@code image/png} o {@code image/webp}
     * @param datos los bytes en base64
     */
    public record Imagen(String tipo, String datos) {
    }

    public record Estado(
            @Size(max = 120) String nombre,
            @NotBlank @Size(max = 20) String distribucion,
            @NotNull @Size(max = 30) List<@Valid Pieza> piezas
    ) {
    }

    public record Pieza(
            @Size(max = 80) String id,
            @NotBlank @Size(max = 20) String tipo,
            @NotBlank @Size(max = 120) String nombre,
            @Size(max = 120) String seccion,
            @NotNull @Size(max = 40) List<@Valid Campo> campos,
            @Size(max = 15) List<@Valid Fila> filas
    ) {
    }

    /** Un registro de la pieza: su id y los primeros datos como texto. */
    public record Fila(@NotBlank @Size(max = 80) String id, @Size(max = 3) List<@Size(max = 100) String> valores) {
    }

    public record Campo(@NotBlank @Size(max = 120) String etiqueta, @NotBlank @Size(max = 20) String tipo) {
    }

    public record Mensaje(
            @NotBlank @Pattern(regexp = "sami|you") String quien,
            @NotBlank @Size(max = 1000) String texto
    ) {
    }
}
