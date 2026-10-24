package com.ensambla.backend.domain.useCase.reporte.generarReporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class GenerarReporte {

    /** Filas que se muestran en pantalla antes de descargar. */
    static final int MAXIMO_VISTA = 50;

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final ReporteService reporteService;

    public GenerarReporteResponse execute(GenerarReporteRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.REPORTES_FILTRADOS, OffsetDateTime.now(clock));
        List<Campo> campos = campoService.listarDeBloque(bloque.getId(), negocioId);
        FiltroReporte filtro = FiltroReporte.armar(
                negocioId,
                bloque.getId(),
                campos,
                request.texto(),
                request.desde(),
                request.hasta(),
                request.campoId(),
                request.valor());
        ReporteRegistros reporte = reporteService.generar(filtro, campos, MAXIMO_VISTA);
        return GenerarReporteResponse.de(reporte, MAXIMO_VISTA);
    }
}
