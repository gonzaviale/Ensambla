package com.ensambla.backend.infrastructure.presentation.dto.negocio;

public record ConfiguracionNegocioWebResponse(String nombre, String rubro,
                                              UbicacionNegocioWeb ubicacion, Boolean comparteDatosZona) {
}
