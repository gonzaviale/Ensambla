package com.ensambla.backend.domain.useCase.metrica.crearMetrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Guarda una métrica en el cuaderno, al final del panel: la que eligió la persona del catálogo de su rubro o la que
 * armó con las listas. Rechaza la receta inválida, la que ya está guardada y las que pasan el máximo por cuaderno.
 */
@RequiredArgsConstructor
public class CrearMetrica {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final MetricaRepository metricaRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MotorMetricas motorMetricas;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public MetricaItem execute(CrearMetricaRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        DefinicionMetrica definicion = new DefinicionMetrica(
                request.nombre(), request.fuente(), request.calculo(), request.agrupar(), request.periodo(),
                request.soloFiado());
        Metrica guardada = transaccionPort.ejecutar(() -> guardar(negocio, request, definicion, ahora));
        return MetricaItem.de(guardada, motorMetricas.calcular(
                definicion, datosDeProyectoService.cargar(proyecto, negocio.getId()), ZonaNegocio.dia(ahora)));
    }

    private Metrica guardar(
            Negocio negocio, CrearMetricaRequest request, DefinicionMetrica definicion, OffsetDateTime ahora) {
        List<Metrica> existentes =
                metricaRepository.findAllByProyectoIdAndNegocioId(request.proyectoId(), negocio.getId());
        if (existentes.size() >= Metrica.MAXIMO_POR_CUADERNO) {
            throw new BusinessRuleException(
                    "Ya tenés " + Metrica.MAXIMO_POR_CUADERNO + " métricas: quitá alguna para agregar otra");
        }
        if (existentes.stream().anyMatch(existente -> existente.getDefinicion().esIgualA(definicion))) {
            throw new BusinessRuleException("Ya tenés esa métrica en el cuaderno");
        }
        int orden = existentes.stream().map(Metrica::getOrden).max(Comparator.naturalOrder()).orElse(-1) + 1;
        return metricaRepository.save(Metrica.nueva(
                negocio.getId(), request.proyectoId(), definicion, orden, request.autoria(), ahora));
    }
}
