package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.dto.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.dto.reporte.GenerarReporteResponse;
import com.ensambla.backend.domain.mapper.reporte.GenerarReporteMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GenerarReporteUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final ReporteService reporteService;

    private final GenerarReporteMapper generarReporteMapper;

    public GenerarReporteResponse execute(GenerarReporteRequest request) {
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
        ReporteRegistros reporte = reporteService.generar(filtro, campos, ReporteService.MAXIMO_VISTA);
        return generarReporteMapper.toResponse(reporte, ReporteService.MAXIMO_VISTA);
    }
}
