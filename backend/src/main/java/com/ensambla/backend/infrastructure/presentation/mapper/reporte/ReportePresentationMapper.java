package com.ensambla.backend.infrastructure.presentation.mapper.reporte;

import com.ensambla.backend.domain.dto.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.dto.reporte.GenerarReporteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.TotalesRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.ReporteResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class ReportePresentationMapper {

    public GenerarReporteRequest toRequest(UUID usuarioId, UUID bloqueId, String texto, LocalDate desde,
                                           LocalDate hasta, UUID campoId, String valor) {
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
