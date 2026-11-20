package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * El objetivo que la persona armó con Sami y lo que aceptó de la propuesta. Con {@code seguimientoManual} se guarda el
 * seguimiento de respaldo y {@code minis} se ignora; si no viene, es que no.
 */
public record GuardarObjetivoConSamiWebRequest(
        @NotBlank(message = "El objetivo es obligatorio")
        @Size(max = 500, message = "El objetivo puede tener hasta 500 caracteres") String texto,
        Boolean seguimientoManual,
        @Size(max = 3, message = "Se guardan hasta 3 mini-objetivos")
        List<@NotNull(message = "El mini-objetivo es obligatorio") @Valid MiniElegidoWebRequest> minis) {
}
