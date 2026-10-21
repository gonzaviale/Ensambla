package com.ensambla.backend.domain.dto.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;

import java.util.UUID;

public record ActualizarNegocioRequest(UUID usuarioId, String nombre, RubroNegocio rubro,
                                       UbicacionNegocioItem ubicacion, Boolean comparteDatosZona) {
}
