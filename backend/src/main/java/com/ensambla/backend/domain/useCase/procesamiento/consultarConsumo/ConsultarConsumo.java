package com.ensambla.backend.domain.useCase.procesamiento.consultarConsumo;

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
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        ConsumoDiario consumo = procesamientoRepository.consumo(negocioId, OffsetDateTime.now(clock).toLocalDate());
        return ConsumoItem.de(consumo, politicaProcesamiento.limiteDiario());
    }
}
