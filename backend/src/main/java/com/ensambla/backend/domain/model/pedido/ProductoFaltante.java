package com.ensambla.backend.domain.model.pedido;

/**
 * Algo que el cliente pidió y el comercio no tiene.
 *
 * @param pedido  lo que dijo el cliente
 * @param similar un producto parecido que sí tiene, para recomendárselo; null si no hay ninguno
 */
public record ProductoFaltante(String pedido, ProductoEnStock similar) {
}
