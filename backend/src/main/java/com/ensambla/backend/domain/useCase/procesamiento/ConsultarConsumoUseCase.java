package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConsumoItem;
import com.ensambla.backend.domain.mapper.procesamiento.ConsultarConsumoMapper;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.procesamiento.ProcesamientoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarConsumoUseCase {

    private final NegocioService negocioService;
    private final ProcesamientoService procesamientoService;
    private final ConsultarConsumoMapper consultarConsumoMapper;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public ConsumoItem execute(UUID usuarioId) {
        UUID negocioId = negocioService.obtenerDelUsuario(usuarioId);
        ConsumoDiario consumo = procesamientoService.consumo(negocioId, OffsetDateTime.now(clock));
        return consultarConsumoMapper.toResponse(consumo, politicaProcesamiento.limiteDiario());
    }
}
