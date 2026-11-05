package com.ensambla.backend.infrastructure.presentation.dto.promocion;

import java.util.List;

public record SugerenciasDePromocionResponse(List<PromocionResponse> promociones, List<CanalResponse> canales) {
}
