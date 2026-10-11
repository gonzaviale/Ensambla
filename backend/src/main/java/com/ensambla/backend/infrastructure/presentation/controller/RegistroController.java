package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.registro.CrearRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EditarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EliminarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarRegistrosUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerFotoOrigenUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerRegistroUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.registro.PaginaRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroRequest;
import com.ensambla.backend.infrastructure.presentation.dto.registro.RegistroResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.registro.RegistroPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Registros")
@RequiredArgsConstructor
public class RegistroController {

    private final CrearRegistroUseCase crearRegistroUseCase;
    private final ListarRegistrosUseCase listarRegistrosUseCase;
    private final ObtenerRegistroUseCase obtenerRegistroUseCase;
    private final EditarRegistroUseCase editarRegistroUseCase;
    private final EliminarRegistroUseCase eliminarRegistroUseCase;
    private final ObtenerFotoOrigenUseCase obtenerFotoOrigenUseCase;
    private final RegistroPresentationMapper presentationMapper;

    @PostMapping("/bloques/{bloqueId}/registros")
    @Operation(summary = "Cargar un registro en un bloque; los valores se indexan por id de campo")
    public ResponseEntity<RegistroResponse> crear(Authentication authentication,
                                                  @PathVariable UUID bloqueId,
                                                  @Valid @RequestBody RegistroRequest request) {
        RegistroResponse response = presentationMapper.toResponse(crearRegistroUseCase.execute(
                presentationMapper.toCrearRequest(usuarioDe(authentication), bloqueId, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bloques/{bloqueId}/registros")
    @Operation(summary = "Listar los registros de un bloque con búsqueda de texto, orden y paginación")
    public ResponseEntity<PaginaRegistrosResponse> listar(Authentication authentication,
                                                          @PathVariable UUID bloqueId,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(required = false) String orden,
                                                          @RequestParam(required = false) String direccion,
                                                          @RequestParam(required = false) Integer pagina,
                                                          @RequestParam(required = false) Integer tamanio) {
        PaginaRegistrosResponse response = presentationMapper.toResponse(listarRegistrosUseCase.execute(
                presentationMapper.toListarRequest(
                        usuarioDe(authentication), bloqueId, q, orden, direccion, pagina, tamanio)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registros/{id}")
    @Operation(summary = "Ver un registro")
    public ResponseEntity<RegistroResponse> obtener(Authentication authentication, @PathVariable UUID id) {
        RegistroResponse response = presentationMapper.toResponse(obtenerRegistroUseCase.execute(
                presentationMapper.toObtenerRequest(usuarioDe(authentication), id)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registros/{id}/foto-origen")
    @Operation(summary = "Ver la foto del cuaderno de la que salió el registro")
    public ResponseEntity<byte[]> fotoOrigen(Authentication authentication, @PathVariable UUID id) {
        return RespuestaFoto.de(obtenerFotoOrigenUseCase.execute(
                presentationMapper.toFotoOrigenRequest(usuarioDe(authentication), id)));
    }

    @PatchMapping("/registros/{id}")
    @Operation(summary = "Editar un registro; los campos que no se envían conservan su valor")
    public ResponseEntity<RegistroResponse> editar(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody RegistroRequest request) {
        RegistroResponse response = presentationMapper.toResponse(editarRegistroUseCase.execute(
                presentationMapper.toEditarRequest(usuarioDe(authentication), id, request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/registros/{id}")
    @Operation(summary = "Eliminar un registro")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID id) {
        eliminarRegistroUseCase.execute(presentationMapper.toEliminarRequest(usuarioDe(authentication), id));
        return ResponseEntity.noContent().build();
    }

    private static UUID usuarioDe(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
