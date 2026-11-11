package com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Las métricas y el flujo de caja de un sistema para la home. Usa el mismo cálculo que el resumen en PDF, así que los
 * números coinciden, y sólo mira el sistema del propio negocio: uno ajeno responde como inexistente.
 */
@RequiredArgsConstructor
public class ObtenerPanelNegocio {

    private final NegocioRepository negocioRepository;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final Clock clock;

    public ObtenerPanelNegocioResponse execute(ObtenerPanelNegocioRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        LocalDate hoy = ZonaNegocio.dia(OffsetDateTime.now(clock));
        PeriodoResumen periodo = PeriodoResumen.ultimosDias(request.dias(), hoy);
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocioId);
        return ObtenerPanelNegocioResponse.de(PanelNegocio.armar(datos, periodo, hoy));
    }
}
