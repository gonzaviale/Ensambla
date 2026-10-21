package com.ensambla.backend.domain.dto.ubicacion;

import java.util.List;

/** {@code localidad} es la principal (null si el mapa no reconoce ninguna); {@code localidades} son todas las válidas. */
public record ResolverLocalidadResponse(String localidad, List<String> localidades) {
}
