package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.campo.crearCampo.CrearCampo;
import com.ensambla.backend.domain.useCase.campo.editarCampo.EditarCampo;
import com.ensambla.backend.domain.useCase.campo.eliminarCampo.EliminarCampo;
import com.ensambla.backend.domain.useCase.campo.listarCampos.ListarCampos;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.campo.CampoWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.campo.CampoPresentationMapper;
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
@Tag(name = "Campos")
@RequiredArgsConstructor
public class CampoController {

    private final ListarCampos listarCampos;

    private final CrearCampo crearCampo;

    private final EditarCampo editarCampo;

    private final EliminarCampo eliminarCampo;

    private final CampoPresentationMapper campoPresentationMapper;

    @GetMapping("/bloques/{bloqueId}/campos")
    @Operation(summary = "Listar los campos de un bloque en el orden del constructor")
    public ResponseEntity<List<CampoResponse>> listar(Authentication authentication, @PathVariable BloqueId bloqueId) {
        return ResponseEntity.ok(campoPresentationMapper.toResponse(listarCampos.execute(
                campoPresentationMapper.toListarRequest(usuarioDe(authentication), bloqueId))));
    }

    @PostMapping("/bloques/{bloqueId}/campos")
    @Operation(summary = "Agregar un campo al final del bloque")
    public ResponseEntity<CampoResponse> crear(
            Authentication authentication,
            @PathVariable BloqueId bloqueId,
            @Valid @RequestBody CampoWebRequest request) {
        CampoResponse response = campoPresentationMapper.toResponse(crearCampo.execute(
                campoPresentationMapper.toCrearRequest(usuarioDe(authentication), bloqueId, request)));
        return ResponseEntity.created(URI.create("/api/campos/" + response.id())).body(response);
    }

    @PatchMapping("/campos/{campoId}")
    @Operation(summary = "Reemplazar la configuración de un campo; el nombre no puede repetirse dentro del bloque")
    public ResponseEntity<CampoResponse> editar(
            Authentication authentication,
            @PathVariable CampoId campoId,
            @Valid @RequestBody CampoWebRequest request) {
        return ResponseEntity.ok(campoPresentationMapper.toResponse(editarCampo.execute(
                campoPresentationMapper.toEditarRequest(usuarioDe(authentication), campoId, request))));
    }

    @DeleteMapping("/campos/{campoId}")
    @Operation(summary = "Eliminar un campo; los registros del bloque se conservan")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable CampoId campoId) {
        eliminarCampo.execute(campoPresentationMapper.toEliminarRequest(usuarioDe(authentication), campoId));
        return ResponseEntity.noContent().build();
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
