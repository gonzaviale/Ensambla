package com.ensambla.backend.domain.useCase.metrica.crearMetrica;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricasDelCuaderno;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

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
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerCuaderno(negocio.getId(), request.proyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Metrica guardada = agregarAlFinalDelPanel(negocio.getId(), request, ahora);
        return MetricaItem.de(guardada, calcularHoy(request.definicion(), proyecto, negocio.getId(), ahora));
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerCuaderno(NegocioId negocioId, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocioId, proyectoId);
    }

    private Metrica agregarAlFinalDelPanel(NegocioId negocioId, CrearMetricaRequest request, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> guardarDespuesDeLasQueHay(negocioId, request, ahora));
    }

    private Metrica guardarDespuesDeLasQueHay(NegocioId negocioId, CrearMetricaRequest request, OffsetDateTime ahora) {
        DefinicionMetrica definicion = request.definicion();
        MetricasDelCuaderno guardadas = metricasDelCuaderno(request.proyectoId(), negocioId);
        guardadas.exigirLugarPara(definicion);
        return guardar(Metrica.nueva(negocioId, request.proyectoId(), definicion, guardadas.siguienteOrden(),
                request.autoria(), ahora));
    }

    private MetricasDelCuaderno metricasDelCuaderno(ProyectoId proyectoId, NegocioId negocioId) {
        return new MetricasDelCuaderno(metricaRepository.findAllByProyectoIdAndNegocioId(proyectoId, negocioId));
    }

    private Metrica guardar(Metrica metrica) {
        return metricaRepository.save(metrica);
    }

    private ResultadoMetrica calcularHoy(DefinicionMetrica definicion, Proyecto proyecto, NegocioId negocioId,
                                         OffsetDateTime ahora) {
        return motorMetricas.calcular(definicion, datosDeProyectoService.cargar(proyecto, negocioId),
                ZonaNegocio.dia(ahora));
    }
}
