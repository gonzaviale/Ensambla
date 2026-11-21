package com.ensambla.backend.domain.useCase.metrica.renombrarMetrica;

import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Le cambia el nombre a una métrica del panel sin tocar lo que calcula. Rechaza el nombre vacío o demasiado largo; una
 * métrica ajena o inexistente responde 404.
 */
@RequiredArgsConstructor
public class RenombrarMetrica {

    private final NegocioRepository negocioRepository;

    private final MetricaRepository metricaRepository;

    private final TransaccionPort transaccionPort;

    public void execute(RenombrarMetricaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Metrica metrica = obtenerMetricaDelCuaderno(negocioId, request.proyectoId(), request.metricaId());
        metrica.renombrar(request.nombre());
        guardar(metrica);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Metrica obtenerMetricaDelCuaderno(NegocioId negocioId, ProyectoId proyectoId, MetricaId metricaId) {
        return metricaRepository.obtener(negocioId, proyectoId, metricaId);
    }

    private void guardar(Metrica metrica) {
        transaccionPort.ejecutar(() -> metricaRepository.save(metrica));
    }
}
