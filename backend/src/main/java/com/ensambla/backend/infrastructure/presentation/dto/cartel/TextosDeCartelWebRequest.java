package com.ensambla.backend.infrastructure.presentation.dto.cartel;

import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record TextosDeCartelWebRequest(
        @NotEmpty @Size(max = 6) List<@NotNull TipoDeCartel> tiposPosibles,
        @NotNull @Size(max = 12) List<@NotNull @Size(max = 80) String> productos,
        @NotNull @Size(max = 30) List<@NotNull BigDecimal> numeros) {
}
