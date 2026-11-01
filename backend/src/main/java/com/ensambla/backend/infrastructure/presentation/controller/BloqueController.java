package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloque;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloque;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloques;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloque;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueResponse;
import com.ensambla.backend.infrastructure.presentation.dto.bloque.BloqueWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.bloque.BloquePresentationMapper;
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
@RequestMapping("/api")
@Tag(name = "Bloques")
@RequiredArgsConstructor
public class BloqueController {

    private final ListarBloques listarBloques;

    private final CrearBloque crearBloque;

    private final RenombrarBloque renombrarBloque;

    private final EliminarBloque eliminarBloque;

    private final BloquePresentationMapper bloquePresentationMapper;

    @GetMapping("/proyectos/{proyectoId}/bloques")
    @Operation(summary = "Listar los bloques de un proyecto en el orden del constructor")
    public ResponseEntity<List<BloqueResponse>> listar(Authentication authentication,
            @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(bloquePresentationMapper.toResponse(listarBloques.execute(
                bloquePresentationMapper.toListarRequest(usuarioDe(authentication), proyectoId))));
    }

    @PostMapping("/proyectos/{proyectoId}/bloques")
    @Operation(summary = "Agregar un bloque al final del proyecto")
    public ResponseEntity<BloqueResponse> crear(
            Authentication authentication,
            @PathVariable ProyectoId proyectoId,
            @Valid @RequestBody BloqueWebRequest request) {
        BloqueResponse response = bloquePresentationMapper.toResponse(crearBloque.execute(
                bloquePresentationMapper.toCrearRequest(usuarioDe(authentication), proyectoId, request)));
        return ResponseEntity.created(URI.create("/api/bloques/" + response.id())).body(response);
    }

    @PatchMapping("/bloques/{bloqueId}")
    @Operation(summary = "Renombrar un bloque; el nombre no puede repetirse dentro del proyecto")
    public ResponseEntity<BloqueResponse> renombrar(
            Authentication authentication,
            @PathVariable BloqueId bloqueId,
            @Valid @RequestBody BloqueWebRequest request) {
        return ResponseEntity.ok(bloquePresentationMapper.toResponse(renombrarBloque.execute(
                bloquePresentationMapper.toRenombrarRequest(usuarioDe(authentication), bloqueId, request))));
    }

    @DeleteMapping("/bloques/{bloqueId}")
    @Operation(summary = "Eliminar un bloque junto con sus campos y registros")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable BloqueId bloqueId) {
        eliminarBloque.execute(bloquePresentationMapper.toEliminarRequest(usuarioDe(authentication), bloqueId));
        return ResponseEntity.noContent().build();
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
