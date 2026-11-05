package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicio;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse;
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

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/resumen")
@Tag(name = "Reportes")
@RequiredArgsConstructor
public class ResumenController {

    private final ExportarResumenNegocio exportarResumenNegocio;

    private final ObtenerResumenInicio obtenerResumenInicio;

    @GetMapping("/pdf")
    @Operation(summary = "Descargar el resumen del negocio en PDF: ventas, ganancia, stock, gastos, zona y avisos del período")
    public ResponseEntity<byte[]> descargar(
            Authentication authentication,
            @PathVariable ProyectoId proyectoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        ExportarResumenResponse archivo = exportarResumenNegocio.execute(new ExportarResumenRequest(
                UsuarioId.de(authentication.getName()), proyectoId, desde, hasta));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }

    @GetMapping("/inicio")
    @Operation(summary = "Las métricas del inicio de un sistema: ventas de hoy, lo que más salió, lo que falta y lo parado")
    public ResponseEntity<ObtenerResumenInicioResponse> inicio(
            Authentication authentication,
            @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(obtenerResumenInicio.execute(
                new ObtenerResumenInicioRequest(UsuarioId.de(authentication.getName()), proyectoId)));
    }
}
