package com.ensambla.backend.domain.useCase.plan;

import com.ensambla.backend.domain.model.plan.PruebaPremium;

import java.time.OffsetDateTime;

public record PruebaPremiumResponse(OffsetDateTime inicioEn, OffsetDateTime finEn,
                                   int paginasIaUtilizadas, int limitePaginasIa) {

    public static PruebaPremiumResponse de(PruebaPremium prueba) {
        return new PruebaPremiumResponse(prueba.inicioEn(), prueba.finEn(),
                prueba.paginasIa(), PruebaPremium.LIMITE_PAGINAS_IA);
    }
}
