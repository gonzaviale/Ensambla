package com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad;

import java.math.BigDecimal;

public record ResolverLocalidadRequest(BigDecimal latitud, BigDecimal longitud) {
}
