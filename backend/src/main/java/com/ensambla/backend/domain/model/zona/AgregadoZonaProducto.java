package com.ensambla.backend.domain.model.zona;

import java.math.BigDecimal;

/**
 * Precio y ventas de un producto en una zona. Es un dato agregado y anónimo: nunca identifica a un comercio.
 *
 * @param comercios      cuántos comercios aportan el precio
 * @param comerciosVenden cuántos comercios lo venden
 * @param unidadesSemana unidades por semana que vende un comercio promedio
 */
public record AgregadoZonaProducto(
        String nombre,
        int comercios,
        int comerciosVenden,
        BigDecimal precioMinimo,
        BigDecimal precioPromedio,
        BigDecimal precioMaximo,
        BigDecimal unidadesSemana) {
}
