package com.ensambla.backend.domain.dto.negocio;

import com.ensambla.backend.domain.model.enums.RubroNegocio;

/** {@code zona} es sólo el nombre de la zona asignada; nunca datos de otros comercios. */
public record ConfiguracionNegocioResponse(String nombre, RubroNegocio rubro, UbicacionNegocioItem ubicacion,
                                           Boolean comparteDatosZona, String zona) {
}
