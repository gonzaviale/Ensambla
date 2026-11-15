package com.ensambla.backend.infrastructure.presentation.dto.negocio;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearNegocioWebRequest(
        @NotBlank @Size(max = 120) String nombre,
        RubroNegocio rubro,
        @Valid UbicacionNegocioWeb ubicacion) {
}
