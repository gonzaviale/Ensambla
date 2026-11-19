package com.ensambla.backend.domain.useCase.procesamiento.reintentarLectura;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.procesamiento.UsuarioLoteRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ReintentarLectura {

    private final NegocioRepository negocioRepository;
    private final ControlPlanService controlPlanService;
    private final ProcesamientoRepository procesamientoRepository;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public void execute(UsuarioLoteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        exigirQueElLoteExista(negocioId, request.loteId());
        exigirCupoDePaginasDelPlan(negocioId, ahora);
        exigirCupoDiario(negocioId, ahora);
        reencolar(negocioId, request.loteId(), ahora);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirQueElLoteExista(NegocioId negocioId, LoteId loteId) {
        procesamientoRepository.obtenerEstado(negocioId, loteId);
    }

    private void exigirCupoDePaginasDelPlan(NegocioId negocioId, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(negocioId, 1, ahora);
    }

    private void exigirCupoDiario(NegocioId negocioId, OffsetDateTime ahora) {
        ConsumoDiario consumo = obtenerConsumoDelDia(negocioId, ahora);
        if (consumo.paginas() + consumo.reservadas() >= politicaProcesamiento.limiteDiario()) {
            throw new BusinessRuleException(PoliticaProcesamiento.LIMITE_DIARIO_AGOTADO);
        }
    }

    private ConsumoDiario obtenerConsumoDelDia(NegocioId negocioId, OffsetDateTime ahora) {
        return procesamientoRepository.consumo(negocioId, ZonaNegocio.dia(ahora));
    }

    private void reencolar(NegocioId negocioId, LoteId loteId, OffsetDateTime ahora) {
        if (!procesamientoRepository.reencolar(negocioId, loteId, ahora)) {
            throw new BusinessRuleException("Sólo se puede reintentar un lote en estado ERROR");
        }
    }
}
