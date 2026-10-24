package com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import com.ensambla.backend.domain.useCase.procesamiento.ConsumoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ConsultarConsumo {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public ConsumoItem execute(UsuarioId usuarioId) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(usuarioId);
        ConsumoDiario consumo = procesamientoService.consumo(negocioId, OffsetDateTime.now(clock));
        return ConsumoItem.de(consumo, politicaProcesamiento.limiteDiario());
    }
}
