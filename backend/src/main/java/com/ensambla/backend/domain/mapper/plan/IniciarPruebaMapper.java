package com.ensambla.backend.domain.mapper.plan;

import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.model.plan.PruebaPremium;

public class IniciarPruebaMapper {

    public PruebaPremiumResponse toResponse(PruebaPremium prueba) {
        return new PruebaPremiumResponse(prueba.inicioEn(), prueba.finEn(),
                prueba.paginasIa(), PruebaPremium.LIMITE_PAGINAS_IA);
    }
}
