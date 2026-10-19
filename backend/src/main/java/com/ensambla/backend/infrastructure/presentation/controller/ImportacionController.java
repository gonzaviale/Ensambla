package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.lectura.LeerImagenUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.LecturaImagenResponse;
import com.ensambla.backend.infrastructure.presentation.dto.importacion.PedidoLecturaRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.importacion.ImportacionPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Importaciones")
public class ImportacionController {

    private final LeerImagenUseCase leerImagenUseCase;
    private final ImportacionPresentationMapper importacionPresentationMapper;

    @PostMapping(value = "/api/importaciones/lectura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Leer una foto contra un bloque con IA: devuelve registros y columnas nuevas para revisar, sin guardar nada")
    public ResponseEntity<LecturaImagenResponse> leer(
            @RequestPart("imagen") MultipartFile imagen, @RequestPart("pedido") @Valid PedidoLecturaRequest pedido) {
        return ResponseEntity.ok(importacionPresentationMapper.toResponse(
                leerImagenUseCase.execute(importacionPresentationMapper.toRequest(imagen, pedido)), pedido));
    }
}
