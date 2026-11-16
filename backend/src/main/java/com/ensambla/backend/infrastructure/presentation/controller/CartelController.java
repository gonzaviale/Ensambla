package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartel;
import com.ensambla.backend.domain.useCase.cartel.exportarCartel.ExportarCartelResponse;
import com.ensambla.backend.domain.useCase.cartel.redactarTextosDeCartel.RedactarTextosDeCartel;
import com.ensambla.backend.domain.useCase.cartel.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.ExportarCartelWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.PromocionesSugeridasResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.TextosDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.TextosDeCartelWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.cartel.CartelPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carteles")
@Tag(name = "Carteles")
@RequiredArgsConstructor
public class CartelController {

    private final ArmarOpcionesDeCartel armarOpcionesDeCartel;

    private final RedactarTextosDeCartel redactarTextosDeCartel;

    private final SugerirPromociones sugerirPromociones;

    private final ExportarCartel exportarCartel;

    private final CartelPresentationMapper cartelPresentationMapper;

    @PostMapping("/opciones")
    @Operation(summary = "Armar las 3 opciones de cartel con la marca del negocio de la sesión")
    public ResponseEntity<OpcionesDeCartelResponse> armarOpciones(
            Authentication authentication, @Valid @RequestBody OpcionesDeCartelWebRequest request) {
        return ResponseEntity.ok(cartelPresentationMapper.toResponse(armarOpcionesDeCartel.execute(
                cartelPresentationMapper.toArmarRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @PostMapping("/textos")
    @Operation(summary = "Redactar el titular, la bajada y el texto del posteo (con IA o con los textos de reserva)")
    public ResponseEntity<TextosDeCartelResponse> redactarTextos(
            Authentication authentication, @Valid @RequestBody TextosDeCartelWebRequest request) {
        return ResponseEntity.ok(cartelPresentationMapper.toResponse(redactarTextosDeCartel.execute(
                cartelPresentationMapper.toRedactarRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @GetMapping("/promociones-sugeridas")
    @Operation(summary = "Sugerir qué promocionar con lo que el motor de avisos encontró en el negocio de la sesión")
    public ResponseEntity<PromocionesSugeridasResponse> sugerirPromociones(Authentication authentication) {
        return ResponseEntity.ok(cartelPresentationMapper.toResponse(sugerirPromociones.execute(
                cartelPresentationMapper.toSugerirRequest(UsuarioId.de(authentication.getName())))));
    }

    @PostMapping("/exportar")
    @Operation(summary = "Bajar el cartel elegido: PDF para imprimir (A4, A5) o PNG para redes (cuadrado, vertical)")
    public ResponseEntity<byte[]> exportar(
            Authentication authentication, @Valid @RequestBody ExportarCartelWebRequest request) {
        ExportarCartelResponse cartel = exportarCartel.execute(
                cartelPresentationMapper.toExportarRequest(UsuarioId.de(authentication.getName()), request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cartel.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(cartel.nombreDeArchivo()).build().toString())
                .body(cartel.contenido());
    }
}
