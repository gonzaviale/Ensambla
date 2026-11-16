package com.ensambla.backend.infrastructure.presentation.dto.catalogo;

public record ProductoCatalogoWebResponse(ProductoWeb producto) {

    public record ProductoWeb(String codigo, String nombre, String marca, String presentacion, String imagen) {
    }
}
