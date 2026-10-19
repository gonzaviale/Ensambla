package com.ensambla.backend.infrastructure.presentation.dto.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos ausentes o null conservan su valor; ubicacion exige sus cuatro datos si se envía. */
public record ActualizarNegocioWebRequest(
        @Size(max = 120) @Pattern(regexp = "(?s).*\\S.*") String nombre,
        RubroNegocio rubro,
        @Valid UbicacionNegocioWeb ubicacion,
        Boolean comparteDatosZona) {
}
