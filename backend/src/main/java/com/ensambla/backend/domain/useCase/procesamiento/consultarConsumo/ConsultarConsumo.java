package com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.useCase.procesamiento.ConsumoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ConsultarConsumo {

    private final NegocioRepository negocioRepository;
    private final ProcesamientoRepository procesamientoRepository;
    private final PoliticaProcesamiento politicaProcesamiento;
    private final Clock clock;

    public ConsumoItem execute(UsuarioId usuarioId) {
        NegocioId negocioId = obtenerNegocioDelUsuario(usuarioId);
        ConsumoDiario consumo = obtenerConsumoDeHoy(negocioId);
        return ConsumoItem.de(consumo, politicaProcesamiento.limiteDiario());
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private ConsumoDiario obtenerConsumoDeHoy(NegocioId negocioId) {
        return procesamientoRepository.consumo(negocioId, ZonaNegocio.dia(OffsetDateTime.now(clock)));
    }
}
