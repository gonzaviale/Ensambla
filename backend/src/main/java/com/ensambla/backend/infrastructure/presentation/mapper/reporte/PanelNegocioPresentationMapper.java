package com.ensambla.backend.infrastructure.presentation.mapper.reporte;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.PanelNegocioResponse;
import org.springframework.stereotype.Component;

@Component
public class PanelNegocioPresentationMapper {

    public ObtenerPanelNegocioRequest toRequest(UsuarioId usuarioId, ProyectoId proyectoId, Integer dias) {
        return new ObtenerPanelNegocioRequest(usuarioId, proyectoId, dias);
    }

    public PanelNegocioResponse toResponse(ObtenerPanelNegocioResponse response) {
        return new PanelNegocioResponse(
                response.desde(),
                response.hasta(),
                response.dias(),
                response.hayDatos(),
                response.metricas() == null ? null : metricas(response.metricas()),
                response.flujoDeCaja().stream().map(PanelNegocioPresentationMapper::semana).toList());
    }

    private static PanelNegocioResponse.Metricas metricas(ObtenerPanelNegocioResponse.Metricas metricas) {
        return new PanelNegocioResponse.Metricas(
                metricas.vendiste(),
                metricas.mercaderia(),
                metricas.gastos(),
                metricas.queda(),
                metricas.teDeben(),
                metricas.margen(),
                metricas.ticketPromedio(),
                metricas.variacion());
    }

    private static PanelNegocioResponse.Semana semana(ObtenerPanelNegocioResponse.Semana semana) {
        return new PanelNegocioResponse.Semana(
                semana.desde(),
                semana.hasta(),
                semana.entradas(),
                semana.salidas(),
                semana.saldo(),
                semana.saldoAcumulado());
    }
}
