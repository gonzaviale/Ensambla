package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.metrica.cambiarGrafico.CambiarGrafico;
import com.ensambla.backend.domain.useCase.metrica.crearMetrica.CrearMetrica;
import com.ensambla.backend.domain.useCase.metrica.eliminarMetrica.EliminarMetrica;
import com.ensambla.backend.domain.useCase.metrica.listarMetricas.ListarMetricas;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.useCase.metrica.listarFuentesDelCuaderno.ListarFuentesDelCuaderno;
import com.ensambla.backend.domain.useCase.metrica.listarSugeridas.ListarSugeridas;
import com.ensambla.backend.domain.useCase.metrica.interpretarPedido.InterpretarPedido;
import com.ensambla.backend.domain.useCase.metrica.moverMetrica.MoverMetrica;
import com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica.PrevisualizarMetrica;
import com.ensambla.backend.domain.useCase.metrica.renombrarMetrica.RenombrarMetrica;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.CambiarGraficoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.CrearMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.InterpretarPedidoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.MoverMetricaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.PedidoInterpretadoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.metrica.RenombrarMetricaWebRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
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

    private final RenombrarMetrica renombrarMetrica;

    private final MoverMetrica moverMetrica;

    private final InterpretarPedido interpretarPedido;

    private final ListarFuentesDelCuaderno listarFuentesDelCuaderno;

    private final CambiarGrafico cambiarGrafico;

    private final MetricaPresentationMapper metricaPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar las métricas del cuaderno, cada una con lo que da hoy")
    public ResponseEntity<List<MetricaResponse>> listar(
            Authentication authentication, @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(metricaPresentationMapper.toResponse(listarMetricas.execute(
                metricaPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName()), proyectoId))));
    }

    @GetMapping("/fuentes")
    @Operation(summary = "Listar de qué se puede armar una métrica: las ventas, gastos o stock que el cuaderno tiene")
    public ResponseEntity<List<FuenteMetrica>> fuentes(
            Authentication authentication, @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(listarFuentesDelCuaderno.execute(
                metricaPresentationMapper.toListarFuentesRequest(UsuarioId.de(authentication.getName()), proyectoId)));
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

    @PatchMapping("/{metricaId}")
    @Operation(summary = "Cambiarle el nombre a una métrica del panel")
    public ResponseEntity<Void> renombrar(
            Authentication authentication, @PathVariable ProyectoId proyectoId, @PathVariable MetricaId metricaId,
            @Valid @RequestBody RenombrarMetricaWebRequest request) {
        renombrarMetrica.execute(metricaPresentationMapper.toRenombrarRequest(
                UsuarioId.de(authentication.getName()), proyectoId, metricaId, request));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{metricaId}/grafico")
    @Operation(summary = "Elegir cómo se dibuja una métrica del panel: número, barras, dona o línea")
    public ResponseEntity<Void> cambiarGrafico(
            Authentication authentication, @PathVariable ProyectoId proyectoId, @PathVariable MetricaId metricaId,
            @Valid @RequestBody CambiarGraficoWebRequest request) {
        cambiarGrafico.execute(metricaPresentationMapper.toCambiarGraficoRequest(
                UsuarioId.de(authentication.getName()), proyectoId, metricaId, request));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{metricaId}/lugar")
    @Operation(summary = "Cambiar de lugar una métrica del panel; las demás se corren")
    public ResponseEntity<Void> mover(
            Authentication authentication, @PathVariable ProyectoId proyectoId, @PathVariable MetricaId metricaId,
            @Valid @RequestBody MoverMetricaWebRequest request) {
        moverMetrica.execute(metricaPresentationMapper.toMoverRequest(
                UsuarioId.de(authentication.getName()), proyectoId, metricaId, request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/interpretar")
    @Operation(summary = "Entender un pedido en palabras y devolver la métrica que arma, con lo que daría hoy y "
            + "qué se tomó por defecto o no se entendió")
    public ResponseEntity<PedidoInterpretadoResponse> interpretar(
            Authentication authentication, @PathVariable ProyectoId proyectoId,
            @Valid @RequestBody InterpretarPedidoWebRequest request) {
        return ResponseEntity.ok(metricaPresentationMapper.toInterpretadoResponse(interpretarPedido.execute(
                metricaPresentationMapper.toInterpretarRequest(
                        UsuarioId.de(authentication.getName()), proyectoId, request))));
    }
}
