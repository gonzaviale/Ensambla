package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetrica;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetrica;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricas;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridas;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetrica;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.CrearMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.ResultadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.SugeridaResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.metrica.MetricaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/metricas")
@Tag(name = "Métricas a medida")
@RequiredArgsConstructor
public class MetricaController {

    private final ListarMetricas listarMetricas;

    private final ListarSugeridas listarSugeridas;

    private final PrevisualizarMetrica previsualizarMetrica;

    private final CrearMetrica crearMetrica;

    private final EliminarMetrica eliminarMetrica;

    private final MetricaPresentationMapper metricaPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar las métricas del cuaderno, cada una con lo que da hoy")
    public ResponseEntity<List<MetricaResponse>> listar(
            Authentication authentication, @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(metricaPresentationMapper.toResponse(listarMetricas.execute(
                metricaPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName()), proyectoId))));
    }

    @GetMapping("/sugeridas")
    @Operation(summary = "Listar las métricas que el catálogo sugiere al rubro y el cuaderno todavía no tiene")
    public ResponseEntity<List<SugeridaResponse>> sugeridas(
            Authentication authentication, @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(metricaPresentationMapper.toSugeridasResponse(listarSugeridas.execute(
                metricaPresentationMapper.toListarSugeridasRequest(
                        UsuarioId.de(authentication.getName()), proyectoId))));
    }

    @PostMapping("/previsualizar")
    @Operation(summary = "Calcular una métrica con los datos del cuaderno sin guardarla")
    public ResponseEntity<ResultadoResponse> previsualizar(
            Authentication authentication, @PathVariable ProyectoId proyectoId,
            @Valid @RequestBody MetricaWebRequest request) {
        return ResponseEntity.ok(metricaPresentationMapper.toResponse(previsualizarMetrica.execute(
                metricaPresentationMapper.toPrevisualizarRequest(
                        UsuarioId.de(authentication.getName()), proyectoId, request))));
    }

    @PostMapping
    @Operation(summary = "Guardar una métrica en el cuaderno")
    public ResponseEntity<MetricaResponse> crear(
            Authentication authentication, @PathVariable ProyectoId proyectoId,
            @Valid @RequestBody CrearMetricaWebRequest request) {
        MetricaResponse response = metricaPresentationMapper.toResponse(crearMetrica.execute(
                metricaPresentationMapper.toCrearRequest(
                        UsuarioId.de(authentication.getName()), proyectoId, request)));
        return ResponseEntity.created(URI.create("/api/proyectos/" + proyectoId + "/metricas/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{metricaId}")
    @Operation(summary = "Quitar una métrica del cuaderno")
    public ResponseEntity<Void> eliminar(
            Authentication authentication, @PathVariable ProyectoId proyectoId, @PathVariable MetricaId metricaId) {
        eliminarMetrica.execute(metricaPresentationMapper.toEliminarRequest(
                UsuarioId.de(authentication.getName()), proyectoId, metricaId));
        return ResponseEntity.noContent().build();
    }
}
