package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedor;
import com.ensambla.backend.domain.useCase.reposicion.armarPedidoProveedor.ArmarPedidoProveedorResponse;
import com.ensambla.backend.domain.useCase.reposicion.consultarReposicion.ConsultarReposicion;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.PedidoProveedorWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.reposicion.ReposicionResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.reposicion.ReposicionPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/reposicion")
@Tag(name = "Reposición")
@RequiredArgsConstructor
public class ReposicionController {

    private final ConsultarReposicion consultarReposicion;

    private final ArmarPedidoProveedor armarPedidoProveedor;

    private final ReposicionPresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Productos que hay que reponer, agrupados por proveedor y con la cantidad que se sugiere pedir")
    public ResponseEntity<ReposicionResponse> consultar(Authentication authentication,
                                                        @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(presentationMapper.toResponse(consultarReposicion.execute(
                presentationMapper.toConsultarRequest(usuarioDe(authentication), proyectoId))));
    }

    @PostMapping("/pdf")
    @Operation(summary = "Descargar en PDF el pedido a un proveedor con los productos y cantidades elegidos")
    public ResponseEntity<byte[]> descargarPedido(Authentication authentication,
                                                  @PathVariable ProyectoId proyectoId,
                                                  @Valid @RequestBody PedidoProveedorWebRequest request) {
        ArmarPedidoProveedorResponse archivo = armarPedidoProveedor.execute(
                presentationMapper.toArmarRequest(usuarioDe(authentication), proyectoId, request));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .body(archivo.contenido());
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
