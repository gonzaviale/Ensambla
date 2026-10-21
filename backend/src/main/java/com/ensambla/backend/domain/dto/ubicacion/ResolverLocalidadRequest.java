package com.ensambla.backend.domain.dto.ubicacion;

import java.math.BigDecimal;

public record ResolverLocalidadRequest(BigDecimal latitud, BigDecimal longitud) {
}
