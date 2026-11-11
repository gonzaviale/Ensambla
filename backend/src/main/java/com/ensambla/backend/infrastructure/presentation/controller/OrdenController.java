package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden.CambiarEstadoOrden;
import com.ensambla.backend.domain.useCase.orden.listarOrdenes.ListarOrdenes;
import com.ensambla.backend.infrastructure.presentation.dto.orden.CambiarEstadoOrdenWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.orden.OrdenCompraResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.orden.OrdenPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@Tag(name = "Órdenes de compra")
@RequiredArgsConstructor
public class OrdenController {

    private final ListarOrdenes listarOrdenes;

    private final CambiarEstadoOrden cambiarEstadoOrden;

    private final OrdenPresentationMapper ordenPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar las órdenes de compra del negocio de la sesión, las más recientes primero")
    public ResponseEntity<List<OrdenCompraResponse>> listar(
            Authentication authentication, @RequestParam(required = false) EstadoOrdenCompra estado) {
        return ResponseEntity.ok(ordenPresentationMapper.toResponse(listarOrdenes.execute(
                ordenPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName()), estado))));
    }

    @PatchMapping("/{ordenId}/estado")
    @Operation(summary = "Aceptar, rechazar, poner en preparación o dejar lista para retirar una orden del negocio de la sesión")
    public ResponseEntity<OrdenCompraResponse> cambiarEstado(
            Authentication authentication, @PathVariable OrdenCompraId ordenId,
            @Valid @RequestBody CambiarEstadoOrdenWebRequest request) {
        return ResponseEntity.ok(ordenPresentationMapper.toResponse(cambiarEstadoOrden.execute(
                ordenPresentationMapper.toCambiarEstadoRequest(UsuarioId.de(authentication.getName()), ordenId,
                        request))));
    }
}
