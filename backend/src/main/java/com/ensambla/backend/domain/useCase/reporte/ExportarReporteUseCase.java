package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.dto.reporte.ExportarReporteResponse;
import com.ensambla.backend.domain.dto.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.mapper.reporte.ExportarReporteMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.reporte.TablaReporte;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ExportarReporteUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final ReporteService reporteService;

    private final ExportadorReportePort exportadorReportePort;

    private final ExportarReporteMapper exportarReporteMapper;

    private final Clock clock;

    /**
     * El archivo con todas las filas que cumplen el filtro y sus totales (los mismos que se ven en pantalla).
     *
     * @throws BusinessRuleException si son más de {@value ReporteService#MAXIMO_EXPORTACION} filas: no se descarga un
     *                               archivo incompleto, hay que acotar los filtros
     */
    public ExportarReporteResponse execute(GenerarReporteRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
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
        ReporteRegistros reporte = reporteService.generar(filtro, campos, ReporteService.MAXIMO_EXPORTACION);
        if (reporte.truncado()) {
            throw new BusinessRuleException("El reporte tiene más de " + ReporteService.MAXIMO_EXPORTACION
                    + " filas: acotá los filtros para poder descargarlo");
        }
        Map<UUID, String> etiquetas = reporteService.etiquetasDeConexiones(negocioId, campos, reporte.filas());
        TablaReporte tabla = exportarReporteMapper.toTabla(bloque.getNombre(), campos, reporte, etiquetas);
        return exportarReporteMapper.toResponse(
                exportadorReportePort.exportar(tabla), bloque.getNombre(), LocalDate.now(clock));
    }
}
