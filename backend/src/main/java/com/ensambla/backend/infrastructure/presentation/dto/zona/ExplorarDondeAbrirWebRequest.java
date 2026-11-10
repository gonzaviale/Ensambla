package com.ensambla.backend.infrastructure.presentation.dto.zona;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.zona.FactorDondeAbrir;
import com.ensambla.backend.domain.model.zona.PublicoDondeAbrir;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExplorarDondeAbrirWebRequest(
        @NotNull RubroNegocio rubro,
        @NotNull PublicoDondeAbrir publico,
        Map<FactorDondeAbrir, @Min(0) @Max(2) Integer> importancia,
        @Valid LimitesWebRequest limites,
        List<@Valid AlquilerWebRequest> alquileres) {

    public record LimitesWebRequest(
            @PositiveOrZero BigDecimal distanciaMaxKm,
            @PositiveOrZero Integer competenciaMax,
            @PositiveOrZero Integer escuelasMin,
            @PositiveOrZero Integer parquesMin,
            @PositiveOrZero Integer transporteMin,
            @PositiveOrZero BigDecimal alquilerMax) {
    }

    public record AlquilerWebRequest(@NotNull UUID zonaId, @NotNull @Positive BigDecimal valor) {
    }
}
