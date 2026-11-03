package com.ensambla.backend.domain.model.precio;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Un cambio del precio de venta de un producto del inventario.
 *
 * @param anterior el precio que tenía; {@code null} si es el primero que se carga
 * @param nuevo    el precio que quedó
 */
public record CambioPrecio(
        NegocioId negocioId,
        RegistroId registroId,
        BigDecimal anterior,
        BigDecimal nuevo,
        OffsetDateTime cambiadoEn) {

    /**
     * El cambio entre dos precios, o {@code null} si no hay nada que registrar: el precio nuevo no es positivo (se
     * borró o no es un precio) o es igual al anterior.
     */
    public static CambioPrecio entre(NegocioId negocioId, RegistroId registroId, BigDecimal anterior, BigDecimal nuevo,
                                     OffsetDateTime ahora) {
        if (nuevo == null || nuevo.signum() <= 0) {
            return null;
        }
        if (anterior != null && anterior.compareTo(nuevo) == 0) {
            return null;
        }
        return new CambioPrecio(negocioId, registroId, anterior != null && anterior.signum() > 0 ? anterior : null,
                nuevo, ahora);
    }

    /** {@code true} si el precio subió: el primer precio que se carga no es un aumento. */
    public boolean esAumento() {
        return anterior != null && nuevo.compareTo(anterior) > 0;
    }

    /** El día argentino en que cambió el precio. */
    public LocalDate fecha() {
        return ZonaNegocio.dia(cambiadoEn);
    }
}
