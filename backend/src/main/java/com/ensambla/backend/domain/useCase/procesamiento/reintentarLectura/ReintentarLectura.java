package com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ReintentarLectura {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;
    private final ControlPlanService controlPlanService;
    private final ProcesamientoRepository procesamientoRepository;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public void execute(UsuarioLoteRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        procesamientoService.obtener(negocioId, request.loteId());
        controlPlanService.validarPaginasIa(negocioId, 1, ahora);
        validarCupoDiario(negocioId, ahora);
        reencolar(negocioId, request.loteId(), ahora);
    }

    private void validarCupoDiario(NegocioId negocioId, OffsetDateTime ahora) {
        ConsumoDiario consumo = procesamientoService.consumo(negocioId, ahora);
        if (consumo.paginas() + consumo.reservadas() >= politicaProcesamiento.limiteDiario()) {
            throw new BusinessRuleException(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        }
    }

    private void reencolar(NegocioId negocioId, LoteId loteId, OffsetDateTime ahora) {
        if (!procesamientoRepository.reencolar(negocioId, loteId, ahora)) {
            throw new BusinessRuleException("Sólo se puede reintentar un lote en estado ERROR");
        }
    }
}
