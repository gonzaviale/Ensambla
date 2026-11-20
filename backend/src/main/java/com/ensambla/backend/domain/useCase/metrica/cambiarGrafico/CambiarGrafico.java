package com.ensambla.backend.domain.useCase.metrica.cambiarGrafico;

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
 * Cambia cómo se dibuja una métrica del panel (número, barras, dona o línea) sin tocar lo que calcula. Rechaza el
 * gráfico que no sirve para su reparto; una métrica ajena o inexistente responde 404.
 */
@RequiredArgsConstructor
public class CambiarGrafico {

    private final NegocioRepository negocioRepository;

    private final MetricaRepository metricaRepository;

    private final TransaccionPort transaccionPort;

    public void execute(CambiarGraficoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Metrica metrica = obtenerMetricaDelCuaderno(negocioId, request.proyectoId(), request.metricaId());
        metrica.cambiarGrafico(request.grafico());
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
