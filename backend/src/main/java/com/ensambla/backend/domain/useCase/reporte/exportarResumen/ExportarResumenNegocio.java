package com.ensambla.backend.domain.useCase.reporte.exportarResumen;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * El resumen del negocio en PDF: lo que vendió, ganó, gastó y le falta en un período, cómo están sus precios frente
 * a la zona y qué le avisó Sami. Es una función de plan (reportes) y sólo mira el sistema del propio negocio.
 */
@RequiredArgsConstructor
public class ExportarResumenNegocio {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final ProyectoRepository proyectoRepository;

    private final DatosDeProyectoService datosDeProyectoService;

    private final ZonaRepository zonaRepository;

    private final AgregadoZonaRepository agregadoZonaRepository;

    private final AvisoRepository avisoRepository;

    private final GeneradorResumenPdfPort generadorResumenPdfPort;

    private final Clock clock;

    public ExportarResumenResponse execute(ExportarResumenRequest request) {
        Negocio negocio = negocioRepository.obtenerNegocioDelUsuario(request.usuarioId());
        NegocioId negocioId = negocio.getId();
        Proyecto proyecto = proyectoRepository.obtener(negocioId, request.proyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.REPORTES_FILTRADOS, ahora);
        LocalDate hoy = ZonaNegocio.dia(ahora);
        PeriodoResumen periodo = PeriodoResumen.armar(request.desde(), request.hasta(), hoy);
        DatosDeProyecto datos = datosDeProyectoService.cargar(proyecto, negocioId);
        ResumenNegocio resumen = ResumenNegocio.armar(
                negocio.getNombre(), datos, zonaDe(negocioId), avisosDe(negocioId, proyecto), periodo, hoy);
        return ExportarResumenResponse.de(generadorResumenPdfPort.generar(resumen), proyecto.getNombre(), hoy);
    }

    /** Los agregados de la zona del negocio, o una zona vacía si todavía no tiene una asignada. */
    private AnalisisZona zonaDe(NegocioId negocioId) {
        return negocioRepository.findZonaIdById(negocioId)
                .flatMap(zonaRepository::findById)
                .map(agregadoZonaRepository::analizar)
                .orElseGet(AnalisisZona::sinZona);
    }

    private List<Aviso> avisosDe(NegocioId negocioId, Proyecto proyecto) {
        return avisoRepository.findByNegocioId(negocioId).stream()
                .filter(aviso -> proyecto.getId().equals(aviso.getProyectoId()))
                .toList();
    }
}
