package com.ensambla.backend.domain.useCase.metrica.listarMetricas;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Las métricas guardadas en un cuaderno, en el orden del panel, cada una con lo que da hoy. El número lo calcula
 * siempre el servidor con las filas reales del cuaderno; si una no se puede calcular trae el motivo.
 */
@RequiredArgsConstructor
public class ListarMetricas {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final MetricaRepository metricaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MotorMetricas motorMetricas;

    private final Clock clock;

    public List<MetricaItem> execute(ListarMetricasRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocio.getId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        return metricaRepository.findAllByProyectoIdAndNegocioId(request.proyectoId(), negocio.getId()).stream()
                .map(metrica -> MetricaItem.de(metrica, motorMetricas.calcular(metrica.getDefinicion(), datos, hoy)))
                .toList();
    }
}
