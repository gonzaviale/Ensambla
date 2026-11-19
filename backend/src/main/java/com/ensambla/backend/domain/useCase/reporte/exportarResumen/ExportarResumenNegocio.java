package com.ensambla.backend.domain.useCase.reporte.exportarResumen;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.PeriodoResumen;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ContextoDeResumenService;
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

    private final ContextoDeResumenService contextoDeResumenService;

    private final GeneradorResumenPdfPort generadorResumenPdfPort;

    private final Clock clock;

    public ExportarResumenResponse execute(ExportarResumenRequest request) {
        Negocio negocio = obtenerNegocioDelUsuario(request.usuarioId());
        Proyecto proyecto = obtenerProyecto(negocio, request.proyectoId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        exigirLaFuncionDeReportes(negocio, ahora);
        LocalDate hoy = ZonaNegocio.dia(ahora);
        ResumenNegocio resumen = armarResumen(request, negocio, proyecto, hoy);
        return ExportarResumenResponse.de(generarPdf(resumen), proyecto.getNombre(), hoy);
    }

    private ResumenNegocio armarResumen(ExportarResumenRequest request, Negocio negocio, Proyecto proyecto,
                                        LocalDate hoy) {
        PeriodoResumen periodo = PeriodoResumen.armar(request.desde(), request.hasta(), hoy);
        DatosDeProyecto datos = cargarDatos(proyecto, negocio);
        return ResumenNegocio.armar(negocio.getNombre(), datos, zonaDe(negocio.getId()),
                avisosDe(negocio.getId(), proyecto), periodo, hoy);
    }

    private Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerNegocioDelUsuario(usuarioId);
    }

    private Proyecto obtenerProyecto(Negocio negocio, ProyectoId proyectoId) {
        return proyectoRepository.obtener(negocio.getId(), proyectoId);
    }

    private void exigirLaFuncionDeReportes(Negocio negocio, OffsetDateTime ahora) {
        controlPlanService.validarFuncion(negocio.getId(), FuncionPlan.REPORTES_FILTRADOS, ahora);
    }

    private DatosDeProyecto cargarDatos(Proyecto proyecto, Negocio negocio) {
        return datosDeProyectoService.cargar(proyecto, negocio.getId());
    }

    private AnalisisZona zonaDe(NegocioId negocioId) {
        return contextoDeResumenService.zonaDe(negocioId);
    }

    private List<Aviso> avisosDe(NegocioId negocioId, Proyecto proyecto) {
        return contextoDeResumenService.avisosDe(negocioId, proyecto);
    }

    private byte[] generarPdf(ResumenNegocio resumen) {
        return generadorResumenPdfPort.generar(resumen);
    }
}
