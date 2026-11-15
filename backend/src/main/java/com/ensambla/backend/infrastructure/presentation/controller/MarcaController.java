package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.marca.guardarKitDeMarca.GuardarKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerKitDeMarca.ObtenerKitDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarca;
import com.ensambla.backend.domain.useCase.marca.obtenerLogoDeMarca.ObtenerLogoDeMarcaResponse;
import com.ensambla.backend.domain.useCase.marca.subirLogoDeMarca.SubirLogoDeMarca;
import com.ensambla.backend.infrastructure.presentation.dto.marca.KitDeMarcaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.marca.KitDeMarcaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.marca.LogoSubidoResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.marca.MarcaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/marca")
@Tag(name = "Marca")
@RequiredArgsConstructor
public class MarcaController {

    private final ObtenerKitDeMarca obtenerKitDeMarca;

    private final GuardarKitDeMarca guardarKitDeMarca;

    private final SubirLogoDeMarca subirLogoDeMarca;

    private final ObtenerLogoDeMarca obtenerLogoDeMarca;

    private final MarcaPresentationMapper marcaPresentationMapper;

    @GetMapping
    @Operation(summary = "Consultar el kit de marca del negocio de la sesión")
    public ResponseEntity<KitDeMarcaResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok(marcaPresentationMapper.toResponse(obtenerKitDeMarca.execute(
                marcaPresentationMapper.toObtenerKitRequest(UsuarioId.de(authentication.getName())))));
    }

    @PutMapping
    @Operation(summary = "Guardar colores, tipografía y tono del kit de marca")
    public ResponseEntity<KitDeMarcaResponse> guardar(
            Authentication authentication, @Valid @RequestBody KitDeMarcaWebRequest request) {
        return ResponseEntity.ok(marcaPresentationMapper.toResponse(guardarKitDeMarca.execute(
                marcaPresentationMapper.toGuardarRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir el logo y recibir los colores que se sugieren a partir de él")
    public ResponseEntity<LogoSubidoResponse> subirLogo(
            Authentication authentication, @RequestPart("logo") MultipartFile logo) throws IOException {
        return ResponseEntity.ok(marcaPresentationMapper.toResponse(subirLogoDeMarca.execute(
                marcaPresentationMapper.toSubirLogoRequest(
                        UsuarioId.de(authentication.getName()), logo.getBytes(), logo.getContentType()))));
    }

    @GetMapping("/logo")
    @Operation(summary = "Descargar el logo del negocio de la sesión")
    public ResponseEntity<byte[]> obtenerLogo(Authentication authentication) {
        ObtenerLogoDeMarcaResponse logo = obtenerLogoDeMarca.execute(
                marcaPresentationMapper.toObtenerLogoRequest(UsuarioId.de(authentication.getName())));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.tipoMime())).body(logo.contenido());
    }
}
