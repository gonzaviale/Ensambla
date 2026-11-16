package com.ensambla.backend.domain.useCase.cartel.sugerirPromociones;

import com.ensambla.backend.domain.useCase.cartel.PromocionSugeridaItem;

import java.util.List;

public record SugerirPromocionesResponse(List<PromocionSugeridaItem> promociones) {
}
