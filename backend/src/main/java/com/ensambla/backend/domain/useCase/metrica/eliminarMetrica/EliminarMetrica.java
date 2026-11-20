package com.ensambla.backend.domain.useCase.metrica.eliminarMetrica;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirQueSeaDelCuaderno(negocioId, request.proyectoId(), request.metricaId());
        quitar(request.metricaId(), negocioId);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirQueSeaDelCuaderno(NegocioId negocioId, ProyectoId proyectoId, MetricaId metricaId) {
        metricaRepository.obtener(negocioId, proyectoId, metricaId);
    }

    private void quitar(MetricaId metricaId, NegocioId negocioId) {
        transaccionPort.ejecutar(() -> metricaRepository.deleteByIdAndNegocioId(metricaId, negocioId));
    }
}
