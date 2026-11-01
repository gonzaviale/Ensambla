package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.lote.obtenerFotoLote.ObtenerFotoLote;
import com.ensambla.backend.domain.useCase.lote.obtenerLote.ObtenerLote;
import com.ensambla.backend.domain.useCase.lote.subirLote.SubirLote;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@Tag(name = "Lotes de fotos")
@RequiredArgsConstructor
public class LoteController {

    private final SubirLote subirLote;

    private final ObtenerLote obtenerLote;

    private final ObtenerFotoLote obtenerFotoLote;

    private final LotePresentationMapper lotePresentationMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir fotos del cuaderno: guarda las fotos y crea un lote pendiente de lectura; bloqueDestinoId (opcional) indica a qué bloque van las filas")
    public ResponseEntity<LoteResponse> subir(
            Authentication authentication, @RequestPart("fotos") List<MultipartFile> fotos,
            @RequestParam(value = "bloqueDestinoId", required = false) BloqueId bloqueDestinoId) {
        LoteResponse response = lotePresentationMapper.toResponse(subirLote.execute(
                lotePresentationMapper.toSubirRequest(UsuarioId.de(authentication.getName()), fotos, bloqueDestinoId)));
        return ResponseEntity.created(URI.create("/api/lotes/" + response.id())).body(response);
    }

    @GetMapping("/{loteId}/fotos/{fotoId}")
    @Operation(summary = "Ver una página subida del lote; sólo la ve el negocio dueño")
    public ResponseEntity<byte[]> foto(
            Authentication authentication, @PathVariable LoteId loteId, @PathVariable FotoId fotoId) {
        return RespuestaFoto.de(obtenerFotoLote.execute(lotePresentationMapper.toFotoRequest(
                UsuarioId.de(authentication.getName()), loteId, fotoId)));
    }

    @GetMapping("/{loteId}")
    @Operation(summary = "Consultar un lote y sus fotos")
    public ResponseEntity<LoteResponse> obtener(Authentication authentication, @PathVariable LoteId loteId) {
        return ResponseEntity.ok(lotePresentationMapper.toResponse(obtenerLote.execute(
                lotePresentationMapper.toObtenerRequest(UsuarioId.de(authentication.getName()), loteId))));
    }
}
