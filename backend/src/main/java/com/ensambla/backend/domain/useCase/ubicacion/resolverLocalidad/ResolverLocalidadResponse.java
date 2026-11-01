package com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad;

import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;

import java.util.List;

/** {@code localidad} es la principal (null si el mapa no reconoce ninguna); {@code localidades} son todas las válidas. */
public record ResolverLocalidadResponse(String localidad, List<String> localidades) {

    public static ResolverLocalidadResponse de(LocalidadesDelPunto localidades) {
        return new ResolverLocalidadResponse(localidades.principal().orElse(null), localidades.nombres());
    }
}
