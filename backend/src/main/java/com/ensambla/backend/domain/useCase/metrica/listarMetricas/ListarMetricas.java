package com.ensambla.backend.domain.useCase.metrica.listarMetricas;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerCuaderno(negocio.getId(), request.proyectoId());
        DatosDeProyecto datos = leerElCuaderno(proyecto, negocio.getId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        return metricasGuardadas(request.proyectoId(), negocio.getId()).stream()
                .map(metrica -> conLoQueDaHoy(metrica, datos, hoy))
                .toList();
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerCuaderno(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private DatosDeProyecto leerElCuaderno(Proyecto proyecto, NegocioId negocioId) {
        return datosDeProyectoService.cargar(proyecto, negocioId);
    }

    private List<Metrica> metricasGuardadas(ProyectoId proyectoId, NegocioId negocioId) {
        return metricaRepository.findAllByProyectoIdAndNegocioId(proyectoId, negocioId);
    }

    private MetricaItem conLoQueDaHoy(Metrica metrica, DatosDeProyecto datos, LocalDate hoy) {
        return MetricaItem.de(metrica, motorMetricas.calcular(metrica.getDefinicion(), datos, hoy));
    }
}
