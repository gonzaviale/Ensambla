package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.dto.plan.IniciarPruebaRequest;
import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PlanPresentationMapper {

    public IniciarPruebaRequest toRequest(UUID usuarioId) {
        return new IniciarPruebaRequest(usuarioId);
    }

    public PruebaResponse toResponse(PruebaPremiumResponse response) {
        return new PruebaResponse(response.inicioEn(), response.finEn(),
                response.paginasIaUtilizadas(), response.limitePaginasIa());
    }
}
