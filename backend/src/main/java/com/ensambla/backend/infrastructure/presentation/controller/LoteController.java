package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.lote.ObtenerLoteUseCase;
import com.ensambla.backend.domain.useCase.lote.SubirLoteUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.lote.LoteResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.lote.LotePresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lotes")
@Tag(name = "Lotes de fotos")
@RequiredArgsConstructor
public class LoteController {

    private final SubirLoteUseCase subirLoteUseCase;

    private final ObtenerLoteUseCase obtenerLoteUseCase;

    private final LotePresentationMapper lotePresentationMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir fotos del cuaderno: crea un lote pendiente de lectura")
    public ResponseEntity<LoteResponse> subir(
            Authentication authentication, @RequestPart("fotos") List<MultipartFile> fotos) {
        LoteResponse response = lotePresentationMapper.toResponse(subirLoteUseCase.execute(
                lotePresentationMapper.toSubirRequest(UUID.fromString(authentication.getName()), fotos)));
        return ResponseEntity.created(URI.create("/api/lotes/" + response.id())).body(response);
    }

    @GetMapping("/{loteId}")
    @Operation(summary = "Consultar un lote con URLs temporales para ver sus fotos")
    public ResponseEntity<LoteResponse> obtener(Authentication authentication, @PathVariable UUID loteId) {
        return ResponseEntity.ok(lotePresentationMapper.toResponse(obtenerLoteUseCase.execute(
                lotePresentationMapper.toObtenerRequest(UUID.fromString(authentication.getName()), loteId))));
    }
}
