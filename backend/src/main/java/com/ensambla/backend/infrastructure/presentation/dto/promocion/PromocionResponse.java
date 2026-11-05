package com.ensambla.backend.infrastructure.presentation.dto.promocion;

import java.util.List;

public record PromocionResponse(
        String clave,
        String titulo,
        String gancho,
        List<ProductoPromocionadoResponse> productos) {
}
