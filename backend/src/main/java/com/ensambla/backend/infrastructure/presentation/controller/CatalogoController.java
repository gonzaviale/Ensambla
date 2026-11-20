package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.catalogo.buscarProducto.BuscarProducto;
import com.ensambla.backend.infrastructure.presentation.dto.catalogo.ProductoCatalogoWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.catalogo.CatalogoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
@Tag(name = "Catálogos de productos")
@RequiredArgsConstructor
public class CatalogoController {

    private final BuscarProducto buscarProducto;

    private final CatalogoPresentationMapper catalogoPresentationMapper;

    @GetMapping("/{fuente}/{codigo}")
    @Operation(summary = "Consultar un código en SEPA u Open Food Facts; requiere sesión")
    public ResponseEntity<ProductoCatalogoWebResponse> buscar(@PathVariable String fuente, @PathVariable String codigo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(catalogoPresentationMapper.toResponse(buscarProducto.execute(
                        catalogoPresentationMapper.toRequest(fuente, codigo))));
    }
}
