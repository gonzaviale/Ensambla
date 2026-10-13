package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.reporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.ExportarReporteUseCase;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.ReporteResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.reporte.ReportePresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/bloques/{bloqueId}/reporte")
@Tag(name = "Reportes")
@RequiredArgsConstructor
public class ReporteController {

    static final MediaType TIPO_XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final GenerarReporteUseCase generarReporteUseCase;

    private final ExportarReporteUseCase exportarReporteUseCase;

    private final ReportePresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Reporte de las filas de un bloque con filtros: las primeras filas y los totales de todas")
    public ResponseEntity<ReporteResponse> generar(
            Authentication authentication,
            @PathVariable UUID bloqueId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID campoId,
            @RequestParam(required = false) String valor) {
        ReporteResponse response = presentationMapper.toResponse(generarReporteUseCase.execute(
                presentationMapper.toRequest(usuarioDe(authentication), bloqueId, q, desde, hasta, campoId, valor)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/descarga")
    @Operation(summary = "Descargar el reporte en XLSX: todas las filas que cumplen los filtros y sus totales")
    public ResponseEntity<byte[]> descargar(
            Authentication authentication,
            @PathVariable UUID bloqueId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID campoId,
            @RequestParam(required = false) String valor) {
        ExportarReporteResponse archivo = exportarReporteUseCase.execute(
                presentationMapper.toRequest(usuarioDe(authentication), bloqueId, q, desde, hasta, campoId, valor));
        return ResponseEntity.ok()
                .contentType(TIPO_XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }

    private static UUID usuarioDe(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
