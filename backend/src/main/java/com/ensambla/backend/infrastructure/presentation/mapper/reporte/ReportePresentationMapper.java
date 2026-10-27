package com.ensambla.backend.infrastructure.presentation.mapper.reporte;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.TotalesRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.ReporteResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportePresentationMapper {

    public GenerarReporteRequest toRequest(UsuarioId usuarioId, BloqueId bloqueId, String texto, LocalDate desde,
                                           LocalDate hasta, CampoId campoId, String valor) {
        return new GenerarReporteRequest(usuarioId, bloqueId, texto, desde, hasta, campoId, valor);
    }

    public ReporteResponse toResponse(GenerarReporteResponse response) {
        return new ReporteResponse(
                response.filas().stream()
                        .map(fila -> new RegistroResponse(
                                fila.id(),
                                fila.bloqueId(),
                                fila.valores(),
                                fila.fotoOrigenId(),
                                fila.creadoEn(),
                                fila.actualizadoEn()))
                        .toList(),
                new TotalesRegistrosResponse(response.totales().cantidad(), response.totales().sumas()),
                response.truncado(),
                response.maximoFilas());
    }
}
