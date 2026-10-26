package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.registro.crearRegistro.CrearRegistro;
import com.ensambla.backend.domain.useCase.registro.editarRegistro.EditarRegistro;
import com.ensambla.backend.domain.useCase.registro.eliminarRegistro.EliminarRegistro;
import com.ensambla.backend.domain.useCase.registro.listarReferencias.ListarReferencias;
import com.ensambla.backend.domain.useCase.registro.listarRegistros.ListarRegistros;
import com.ensambla.backend.domain.useCase.registro.obtenerFotoOrigen.ObtenerFotoOrigen;
import com.ensambla.backend.domain.useCase.registro.obtenerRegistro.ObtenerRegistro;
import com.ensambla.backend.infrastructure.presentation.dto.registro.PaginaRegistrosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.registro.ReferenciaResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Registros")
@RequiredArgsConstructor
public class RegistroController {

    private final CrearRegistro crearRegistro;
    private final ListarRegistros listarRegistros;
    private final ListarReferencias listarReferencias;
    private final ObtenerRegistro obtenerRegistro;
    private final EditarRegistro editarRegistro;
    private final EliminarRegistro eliminarRegistro;
    private final ObtenerFotoOrigen obtenerFotoOrigen;
    private final RegistroPresentationMapper presentationMapper;

    @PostMapping("/bloques/{bloqueId}/registros")
    @Operation(summary = "Cargar un registro en un bloque; los valores se indexan por id de campo")
    public ResponseEntity<RegistroResponse> crear(Authentication authentication,
                                                  @PathVariable BloqueId bloqueId,
                                                  @Valid @RequestBody RegistroRequest request) {
        RegistroResponse response = presentationMapper.toResponse(crearRegistro.execute(
                presentationMapper.toCrearRequest(usuarioDe(authentication), bloqueId, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/bloques/{bloqueId}/registros")
    @Operation(summary = "Listar los registros de un bloque con búsqueda de texto, orden y paginación")
    public ResponseEntity<PaginaRegistrosResponse> listar(Authentication authentication,
                                                          @PathVariable BloqueId bloqueId,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(required = false) String orden,
                                                          @RequestParam(required = false) String direccion,
                                                          @RequestParam(required = false) Integer pagina,
                                                          @RequestParam(required = false) Integer tamanio) {
        PaginaRegistrosResponse response = presentationMapper.toResponse(listarRegistros.execute(
                presentationMapper.toListarRequest(
                        usuarioDe(authentication), bloqueId, q, orden, direccion, pagina, tamanio)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bloques/{bloqueId}/referencias")
    @Operation(summary = "Filas de un bloque para elegir en un campo de conexión: por texto (hasta 20) o por ids")
    public ResponseEntity<List<ReferenciaResponse>> referencias(Authentication authentication,
                                                                @PathVariable BloqueId bloqueId,
                                                                @RequestParam(required = false) String q,
                                                                @RequestParam(required = false) List<RegistroId> ids) {
        List<ReferenciaResponse> response = presentationMapper.toResponse(listarReferencias.execute(
                presentationMapper.toReferenciasRequest(usuarioDe(authentication), bloqueId, q, ids)));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registros/{id}")
    @Operation(summary = "Ver un registro")
    public ResponseEntity<RegistroResponse> obtener(Authentication authentication, @PathVariable UUID id) {
        RegistroResponse response = presentationMapper.toResponse(obtenerRegistro.execute(
                presentationMapper.toObtenerRequest(usuarioDe(authentication), Identificador
                        .opcional(id, RegistroId::de))));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registros/{id}/foto-origen")
    @Operation(summary = "Ver la foto del cuaderno de la que salió el registro")
    public ResponseEntity<byte[]> fotoOrigen(Authentication authentication, @PathVariable UUID id) {
        return RespuestaFoto.de(obtenerFotoOrigen.execute(
                presentationMapper.toFotoOrigenRequest(usuarioDe(authentication), Identificador.opcional(id,
                        RegistroId::de))));
    }

    @PatchMapping("/registros/{id}")
    @Operation(summary = "Editar un registro; los campos que no se envían conservan su valor")
    public ResponseEntity<RegistroResponse> editar(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody RegistroRequest request) {
        RegistroResponse response = presentationMapper.toResponse(editarRegistro.execute(
                presentationMapper.toEditarRequest(usuarioDe(authentication), Identificador.opcional(id,
                        RegistroId::de), request)));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/registros/{id}")
    @Operation(summary = "Eliminar un registro")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID id) {
        eliminarRegistro.execute(presentationMapper.toEliminarRequest(usuarioDe(authentication),
                Identificador.opcional(id, RegistroId::de)));
        return ResponseEntity.noContent().build();
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
