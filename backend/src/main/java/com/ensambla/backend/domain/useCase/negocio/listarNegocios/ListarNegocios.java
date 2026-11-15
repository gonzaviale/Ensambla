package com.ensambla.backend.domain.useCase.negocio.listarNegocios;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.negocio.ListaNegociosResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ListarNegocios {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    public ListaNegociosResponse execute(ListarNegociosRequest request) {
        List<Negocio> negocios = negocioRepository.findAllByDuenioId(request.usuarioId());
        NegocioId activoId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        int limite = controlPlanService.limiteNegocios(request.usuarioId(), OffsetDateTime.now(clock));
        return ListaNegociosResponse.de(negocios, activoId, limite);
    }
}
