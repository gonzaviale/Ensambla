package com.ensambla.backend.domain.useCase.metrica.eliminarMetrica;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

/** Quita una métrica del cuaderno. Una ajena o inexistente responde 404; el resto de las métricas no se tocan. */
@RequiredArgsConstructor
public class EliminarMetrica {

    private final NegocioRepository negocioRepository;

    private final MetricaRepository metricaRepository;

    private final TransaccionPort transaccionPort;

    public void execute(EliminarMetricaRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        metricaRepository.obtener(negocioId, request.proyectoId(), request.metricaId());
        transaccionPort.ejecutar(() -> metricaRepository.deleteByIdAndNegocioId(request.metricaId(), negocioId));
    }
}
