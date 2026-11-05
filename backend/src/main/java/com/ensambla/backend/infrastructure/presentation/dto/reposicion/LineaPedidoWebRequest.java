package com.ensambla.backend.infrastructure.presentation.dto.reposicion;

import com.ensambla.backend.domain.model.registro.RegistroId;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LineaPedidoWebRequest(
        @NotNull RegistroId productoId,
        @NotNull @Positive @DecimalMax("1000000") BigDecimal cantidad
) {
}
