package com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Calcula una receta con los datos del cuaderno sin guardarla: es la vista previa antes de confirmar. */
@RequiredArgsConstructor
public class PrevisualizarMetrica {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final MotorMetricas motorMetricas;

    private final Clock clock;

    public ResultadoItem execute(PrevisualizarMetricaRequest request) {
        DefinicionMetrica definicion = new DefinicionMetrica(
                request.nombre(), request.fuente(), request.calculo(), request.agrupar(), request.periodo(),
                request.soloFiado());
        definicion.validar();
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocio.getId(), request.proyectoId());
        return ResultadoItem.de(motorMetricas.calcular(
                definicion,
                datosDeProyectoService.cargar(proyecto, negocio.getId()),
                ZonaNegocio.dia(OffsetDateTime.now(clock))));
    }
}
