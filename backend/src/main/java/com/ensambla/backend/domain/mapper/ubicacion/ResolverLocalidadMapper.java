package com.ensambla.backend.domain.mapper.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.ResolverLocalidadResponse;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;

public class ResolverLocalidadMapper {

    public ResolverLocalidadResponse toResponse(LocalidadesDelPunto localidades) {
        return new ResolverLocalidadResponse(localidades.principal().orElse(null), localidades.nombres());
    }
}
