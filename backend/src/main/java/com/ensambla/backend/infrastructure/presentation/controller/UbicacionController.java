package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDirecciones;
import com.ensambla.backend.domain.useCase.ubicacion.buscarDirecciones.BuscarDireccionesResponse;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubros;
import com.ensambla.backend.domain.useCase.ubicacion.listarRubros.ListarRubrosResponse;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidad;
import com.ensambla.backend.domain.useCase.ubicacion.resolverLocalidad.ResolverLocalidadResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.BuscarDireccionesWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.DireccionesWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.LocalidadWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.ResolverLocalidadWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.ubicacion.RubrosWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.ubicacion.UbicacionPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ubicacion")
@RequiredArgsConstructor
@Tag(name = "Ubicación", description = "Búsqueda de direcciones y rubros; no modifica el negocio")
public class UbicacionController {

    private final BuscarDirecciones buscarDirecciones;
    private final ResolverLocalidad resolverLocalidad;
    private final ListarRubros listarRubros;
    private final UbicacionPresentationMapper ubicacionPresentationMapper;

    @PostMapping("/direcciones")
    @Operation(summary = "Buscar la dirección comercial en Argentina, sin guardarla")
    public ResponseEntity<DireccionesWebResponse> buscar(@Valid @RequestBody BuscarDireccionesWebRequest request) {
        BuscarDireccionesResponse response = buscarDirecciones.execute(
                ubicacionPresentationMapper.toRequest(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ubicacionPresentationMapper.toResponse(response));
    }

    @PostMapping("/localidad")
    @Operation(summary = "Resolver la localidad que el mapa reconoce en un punto, sin guardarla")
    public ResponseEntity<LocalidadWebResponse> resolverLocalidad(
            @Valid @RequestBody ResolverLocalidadWebRequest request) {
        ResolverLocalidadResponse response = resolverLocalidad.execute(
                ubicacionPresentationMapper.toRequest(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ubicacionPresentationMapper.toResponse(response));
    }

    @GetMapping("/rubros")
    @Operation(summary = "Listar los rubros admitidos por el negocio")
    public ResponseEntity<RubrosWebResponse> listarRubros() {
        ListarRubrosResponse response = listarRubros.execute();
        return ResponseEntity.ok(ubicacionPresentationMapper.toResponse(response));
    }
}
