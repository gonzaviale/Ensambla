package com.ensambla.backend.domain.dto.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;

public record ConfiguracionNegocioResponse(String nombre, RubroNegocio rubro,
                                           UbicacionNegocioItem ubicacion, Boolean comparteDatosZona) {
}
