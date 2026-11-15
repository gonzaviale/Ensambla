package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.negocio.cambiarNegocioActivo.CambiarNegocioActivo;
import com.ensambla.backend.domain.useCase.negocio.crearNegocio.CrearNegocio;
import com.ensambla.backend.domain.useCase.negocio.eliminarNegocio.EliminarNegocio;
import com.ensambla.backend.domain.useCase.negocio.listarNegocios.ListarNegocios;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.CrearNegocioWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ListaNegociosWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.NegocioDeLaCuentaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.negocio.NegociosPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/negocios")
@Tag(name = "Negocios de la cuenta")
@RequiredArgsConstructor
public class NegociosController {

    private final ListarNegocios listarNegocios;
    private final CrearNegocio crearNegocio;
    private final CambiarNegocioActivo cambiarNegocioActivo;
    private final EliminarNegocio eliminarNegocio;
    private final NegociosPresentationMapper mapper;

    @GetMapping
    @Operation(summary = "Listar los negocios de la cuenta y cuál está activo")
    public ResponseEntity<ListaNegociosWebResponse> listar(Authentication authentication) {
        UsuarioId usuarioId = UsuarioId.de(authentication.getName());
        return sinCache(mapper.toResponse(listarNegocios.execute(mapper.toListarRequest(usuarioId))));
    }

    @PostMapping
    @Operation(summary = "Crear un negocio sin cambiar todavía el negocio activo")
    public ResponseEntity<NegocioDeLaCuentaWebResponse> crear(
            Authentication authentication, @Valid @RequestBody CrearNegocioWebRequest request) {
        UsuarioId usuarioId = UsuarioId.de(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(mapper.toResponse(crearNegocio.execute(mapper.toCrearRequest(usuarioId, request))));
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Cambiar el negocio activo de la sesión")
    public ResponseEntity<ListaNegociosWebResponse> activar(
            Authentication authentication, @PathVariable NegocioId id) {
        UsuarioId usuarioId = UsuarioId.de(authentication.getName());
        cambiarNegocioActivo.execute(mapper.toCambiarRequest(usuarioId, id));
        return sinCache(mapper.toResponse(listarNegocios.execute(mapper.toListarRequest(usuarioId))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un negocio y todos sus datos, salvo si es el último de la cuenta")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable NegocioId id) {
        UsuarioId usuarioId = UsuarioId.de(authentication.getName());
        eliminarNegocio.execute(mapper.toEliminarRequest(usuarioId, id));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private ResponseEntity<ListaNegociosWebResponse> sinCache(ListaNegociosWebResponse response) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
