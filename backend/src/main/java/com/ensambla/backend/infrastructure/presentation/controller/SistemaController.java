package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.sistema.SistemaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/sistemas")
@Tag(name = "Sistemas", description = "Mis sistemas: el proyecto completo, con secciones, bloques, columnas y filas")
@RequiredArgsConstructor
public class SistemaController {

    /** Cuándo leyó el cliente los sistemas: en el GET lo manda el servidor y el cliente lo devuelve en cada PUT. */
    public static final String HEADER_LEIDO_EN = "X-Leido-En";

    private final ListarSistemas listarSistemas;

    private final GuardarSistema guardarSistema;

    private final EliminarSistema eliminarSistema;

    private final SistemaPresentationMapper sistemaPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar los sistemas del negocio de la sesión; X-Leido-En dice a qué hora se leyeron")
    public ResponseEntity<List<SistemaWebResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok().header(HEADER_LEIDO_EN, OffsetDateTime.now(ZoneOffset.UTC).toString())
                .body(sistemaPresentationMapper.toResponse(listarSistemas.execute(
                sistemaPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName())))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Crear o reemplazar un sistema entero; se puede repetir sin duplicar nada. Con X-Leido-En no"
            + " elimina las filas que Sami cargó por WhatsApp después de esa hora")
    public ResponseEntity<SistemaWebResponse> guardar(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody SistemaWebRequest request,
            @RequestHeader(value = HEADER_LEIDO_EN, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime leidoEn) {
        return ResponseEntity.ok(sistemaPresentationMapper.toResponse(guardarSistema.execute(
                sistemaPresentationMapper.toGuardarRequest(UsuarioId.de(authentication.getName()), id, request, leidoEn))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un sistema; si ya no existe no falla")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable String id) {
        eliminarSistema.execute(
                sistemaPresentationMapper.toEliminarRequest(UsuarioId.de(authentication.getName()), id));
        return ResponseEntity.noContent().build();
    }
}
