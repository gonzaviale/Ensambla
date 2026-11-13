package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
                        List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
                        BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn) {

    public static VentaCaja nueva(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
            List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal recibido, OffsetDateTime ahora) {
        if (lineas.isEmpty()) {
            throw new BusinessRuleException("Agregá al menos un artículo a la compra");
        }
        BigDecimal total = lineas.stream().map(LineaVentaCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cobrado = recibido == null ? total : recibido;
        if (medio != MedioPagoCaja.EFECTIVO) {
            cobrado = total;
        }
        if (cobrado.compareTo(total) < 0) {
            throw new BusinessRuleException("El efectivo recibido no alcanza el total");
        }
        return new VentaCaja(id, negocioId, proyectoId, huella, List.copyOf(lineas), medio,
                total, cobrado, cobrado.subtract(total), ahora);
    }
}
