package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.reporte.FiltroReporte;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.LocalDate;
import java.util.List;

/**
 * Pedido de un reporte de las filas de un bloque (para ver o para descargar). Todos los filtros son opcionales:
 * texto, rango de fechas de carga (inclusive) y un valor de una columna de opción o de sí/no.
 */
public record GenerarReporteRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        String texto,
        LocalDate desde,
        LocalDate hasta,
        CampoId campoId,
        String valor) {

    /** El filtro de este pedido, validado contra los campos de la tabla. */
    public FiltroReporte comoFiltro(NegocioId negocioId, Bloque bloque, List<Campo> campos) {
        return FiltroReporte.armar(negocioId, bloque.getId(), campos, texto, desde, hasta, campoId, valor);
    }
}
