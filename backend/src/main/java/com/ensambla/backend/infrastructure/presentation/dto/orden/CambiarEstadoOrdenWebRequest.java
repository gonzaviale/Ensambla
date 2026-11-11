package com.ensambla.backend.infrastructure.presentation.dto.orden;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** @param motivo por qué se rechaza; sólo se usa al rechazar y puede faltar */
public record CambiarEstadoOrdenWebRequest(
        @NotNull EstadoOrdenCompra estado,
        @Size(max = 300) String motivo
) {
}
