package com.ensambla.backend.domain.useCase.metrica.moverMetrica;

import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.metrica.MetricasDelCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Cambia de lugar una métrica del panel y corre las demás, sin dejar huecos. Rechaza un lugar que no está en el panel;
 * una métrica ajena o inexistente responde 404.
 */
@RequiredArgsConstructor
public class MoverMetrica {

    private final NegocioRepository negocioRepository;

    private final MetricaRepository metricaRepository;

    private final TransaccionPort transaccionPort;

    public void execute(MoverMetricaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        exigirQueSeaDelCuaderno(negocioId, request.proyectoId(), request.metricaId());
        MetricasDelCuaderno panel = metricasDelCuaderno(request.proyectoId(), negocioId);
        guardarEnOrden(panel.conLaMetricaEnElLugar(request.metricaId(), request.lugar()));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private void exigirQueSeaDelCuaderno(NegocioId negocioId, ProyectoId proyectoId, MetricaId metricaId) {
        metricaRepository.obtener(negocioId, proyectoId, metricaId);
    }

    private MetricasDelCuaderno metricasDelCuaderno(ProyectoId proyectoId, NegocioId negocioId) {
        return new MetricasDelCuaderno(metricaRepository.findAllByProyectoIdAndNegocioId(proyectoId, negocioId));
    }

    private void guardarEnOrden(List<Metrica> panel) {
        transaccionPort.ejecutar(() -> panel.forEach(metricaRepository::save));
    }
}
