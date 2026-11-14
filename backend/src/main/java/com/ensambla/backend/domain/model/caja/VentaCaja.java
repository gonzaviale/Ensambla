package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
                        List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
                        BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn,
                        List<PagoCaja> pagos, BloqueId inventarioId) {

    public VentaCaja {
        lineas = List.copyOf(lineas);
        pagos = List.copyOf(pagos);
    }

    public VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
            List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
            BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn) {
        this(id, negocioId, proyectoId, huella, lineas, medio, total, recibido, vuelto, creadoEn,
                List.of(new PagoCaja(medio, total)), null);
    }

    public static VentaCaja nueva(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
            List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal recibido, OffsetDateTime ahora) {
        return nueva(id, negocioId, proyectoId, null, huella, lineas, medio, recibido, ahora, null);
    }

    public static VentaCaja nueva(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId,
            BloqueId inventarioId, String huella, List<LineaVentaCaja> lineas, MedioPagoCaja medio,
            BigDecimal recibido, OffsetDateTime ahora, List<PagoCaja> pagosElegidos) {
        if (lineas.isEmpty()) {
            throw new BusinessRuleException("Agregá al menos un artículo a la compra");
        }
        BigDecimal total = lineas.stream().map(LineaVentaCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PagoCaja> pagos = pagosElegidos == null ? List.of(new PagoCaja(medio, total)) : pagosElegidos;
        validarPagos(pagos, total);
        MedioPagoCaja medioReal = pagos.size() == 1 ? pagos.getFirst().medio() : MedioPagoCaja.MIXTO;
        BigDecimal efectivo = pagos.stream().filter(pago -> pago.medio() == MedioPagoCaja.EFECTIVO)
                .map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal entregado = efectivo.signum() == 0 || recibido == null ? efectivo : recibido;
        if (entregado.compareTo(efectivo) < 0 || entregado.scale() > 2) {
            throw new BusinessRuleException("El efectivo recibido debe alcanzar la parte en efectivo");
        }
        return new VentaCaja(id, negocioId, proyectoId, huella, lineas, medioReal, total,
                total.add(entregado.subtract(efectivo)), entregado.subtract(efectivo), ahora, pagos, inventarioId);
    }

    private static void validarPagos(List<PagoCaja> pagos, BigDecimal total) {
        if (pagos.isEmpty() || pagos.size() > 3 ||
                pagos.stream().map(PagoCaja::medio).distinct().count() != pagos.size()) {
            throw new BusinessRuleException("Elegí hasta tres medios de pago diferentes");
        }
        BigDecimal suma = pagos.stream().map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(total) != 0) {
            throw new BusinessRuleException("Los pagos deben cubrir exactamente el total de la compra");
        }
    }
}
