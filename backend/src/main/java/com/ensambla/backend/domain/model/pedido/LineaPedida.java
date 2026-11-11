package com.ensambla.backend.domain.model.pedido;

import java.math.BigDecimal;

/**
 * Una cosa que el cliente pidió, tal como la entendió la IA y todavía sin buscarla en el stock.
 *
 * @param producto lo que dijo («yerba», «aceite girasol»)
 * @param cantidad cuántas unidades; null si no la dijo
 */
public record LineaPedida(String producto, BigDecimal cantidad) {
}
