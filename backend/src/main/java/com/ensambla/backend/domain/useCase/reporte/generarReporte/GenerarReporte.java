package com.ensambla.backend.domain.useCase.reporte.generarReporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    private final BloqueRepository bloqueRepository;

    private final CampoRepository campoRepository;

    private final ReporteService reporteService;

    public GenerarReporteResponse execute(GenerarReporteRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Bloque bloque = obtenerBloque(request, negocioId);
        exigirLaFuncionDeReportes(negocioId);
        List<Campo> campos = obtenerCampos(bloque, negocioId);
        ReporteRegistros reporte = generarVistaPrevia(request.comoFiltro(negocioId, bloque, campos), campos);
        return GenerarReporteResponse.de(reporte, MAXIMO_VISTA);
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Bloque obtenerBloque(GenerarReporteRequest request, NegocioId negocioId) {
        return bloqueRepository.obtener(request.bloqueId(), negocioId);
    }

    private void exigirLaFuncionDeReportes(NegocioId negocioId) {
        controlPlanService.validarFuncion(negocioId, FuncionPlan.REPORTES_FILTRADOS, OffsetDateTime.now(clock));
    }

    private List<Campo> obtenerCampos(Bloque bloque, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloque.getId(), negocioId);
    }

    private ReporteRegistros generarVistaPrevia(FiltroReporte filtro, List<Campo> campos) {
        return reporteService.generar(filtro, campos, MAXIMO_VISTA);
    }
}
