package com.ensambla.backend.infrastructure.presentation.dto.cartel;

import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OpcionesDeCartelWebRequest(
        @NotNull TipoDeCartel tipo,
        @NotNull FormatoDeCartel formato,
        @NotNull @Size(min = 1, max = 40) String titular,
        @Size(max = 90) String bajada,
        @Size(max = 20) String protagonista,
        @Size(max = 60) String detalle,
        @Size(max = 10) List<@NotNull @Size(max = 40) String> lineas) {
}
