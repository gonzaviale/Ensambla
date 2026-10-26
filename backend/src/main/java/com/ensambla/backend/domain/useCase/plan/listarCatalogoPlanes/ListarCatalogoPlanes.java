package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarCatalogoPlanes {

    private final PlanRepository planRepository;

    private final TransaccionPort transaccionPort;

    public ListarCatalogoPlanesResponse execute() {
        return transaccionPort.ejecutar(() -> ListarCatalogoPlanesResponse.de(List.of(
                planRepository.obtener(CodigoPlan.GRATUITO), planRepository.obtener(CodigoPlan.PREMIUM))));
    }
}
