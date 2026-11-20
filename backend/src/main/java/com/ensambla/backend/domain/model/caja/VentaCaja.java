package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public record VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
                        List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
                        BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn,
                        List<PagoCaja> pagos, BloqueId inventarioId,
                        RegistroId clienteId, String clienteNombre, List<PagoCaja> pagosIniciales) {

    public VentaCaja {
        lineas = List.copyOf(lineas);
        pagos = List.copyOf(pagos);
        pagosIniciales = pagosIniciales == null ? pagos : List.copyOf(pagosIniciales);
    }

    public VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
            List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
            BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn,
            List<PagoCaja> pagos, BloqueId inventarioId, RegistroId clienteId, String clienteNombre) {
        this(id, negocioId, proyectoId, huella, lineas, medio, total, recibido, vuelto, creadoEn,
                pagos, inventarioId, clienteId, clienteNombre, pagos);
    }

    public VentaCaja conCobro(List<PagoCaja> nuevos) {
        Map<MedioPagoCaja, BigDecimal> importes = new LinkedHashMap<>();
        pagos.forEach(pago -> importes.merge(pago.medio(), pago.importe(), BigDecimal::add));
        nuevos.forEach(pago -> importes.merge(pago.medio(), pago.importe(), BigDecimal::add));
        List<PagoCaja> acumulados = importes.entrySet().stream()
                .map(entrada -> new PagoCaja(entrada.getKey(), entrada.getValue())).toList();
        validarPagos(acumulados, total, clienteId);
        MedioPagoCaja medioActual = acumulados.size() == 1 ? acumulados.getFirst().medio() : MedioPagoCaja.MIXTO;
        return new VentaCaja(id, negocioId, proyectoId, huella, lineas, medioActual, total, recibido, vuelto,
                creadoEn, acumulados, inventarioId, clienteId, clienteNombre, pagosIniciales);
    }

    public VentaCaja(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId, String huella,
            List<LineaVentaCaja> lineas, MedioPagoCaja medio, BigDecimal total,
            BigDecimal recibido, BigDecimal vuelto, OffsetDateTime creadoEn,
            List<PagoCaja> pagos, BloqueId inventarioId) {
        this(id, negocioId, proyectoId, huella, lineas, medio, total, recibido, vuelto, creadoEn,
                pagos, inventarioId, null, null);
    }

    public BigDecimal cobrado() {
        return pagos.stream().map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal importeEn(MedioPagoCaja medioDePago) {
        return pagos.stream().filter(pago -> pago.medio() == medioDePago)
                .map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        return total.subtract(cobrado());
    }

    public EstadoVentaCaja estado() {
        if (saldo().signum() == 0) {
            return EstadoVentaCaja.PAGADA;
        }
        return cobrado().signum() == 0 ? EstadoVentaCaja.PENDIENTE : EstadoVentaCaja.PARCIAL;
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
        return nueva(id, negocioId, proyectoId, inventarioId, huella, lineas, medio,
                recibido, ahora, pagosElegidos, null, null);
    }

    public static VentaCaja nueva(VentaCajaId id, NegocioId negocioId, ProyectoId proyectoId,
            BloqueId inventarioId, String huella, List<LineaVentaCaja> lineas, MedioPagoCaja medio,
            BigDecimal recibido, OffsetDateTime ahora, List<PagoCaja> pagosElegidos,
            RegistroId clienteId, String clienteNombre) {
        if (lineas.isEmpty()) {
            throw new BusinessRuleException("Agregá al menos un artículo a la compra");
        }
        BigDecimal total = lineas.stream().map(LineaVentaCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PagoCaja> pagos = pagosElegidos == null ? List.of(new PagoCaja(medio, total)) : pagosElegidos;
        validarPagos(pagos, total, clienteId);
        MedioPagoCaja medioReal = pagos.isEmpty() ? medio
                : pagos.size() == 1 ? pagos.getFirst().medio() : MedioPagoCaja.MIXTO;
        BigDecimal cobrado = pagos.stream().map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal efectivo = pagos.stream().filter(pago -> pago.medio() == MedioPagoCaja.EFECTIVO)
                .map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal entregado = efectivo.signum() == 0 || recibido == null ? efectivo : recibido;
        if (entregado.compareTo(efectivo) < 0 || entregado.scale() > 2) {
            throw new BusinessRuleException("El efectivo recibido debe alcanzar la parte en efectivo");
        }
        return new VentaCaja(id, negocioId, proyectoId, huella, lineas, medioReal, total,
                cobrado.add(entregado.subtract(efectivo)), entregado.subtract(efectivo), ahora, pagos, inventarioId,
                clienteId, clienteNombre);
    }

    private static void validarPagos(List<PagoCaja> pagos, BigDecimal total, RegistroId clienteId) {
        if (pagos.size() > 3 ||
                pagos.stream().map(PagoCaja::medio).distinct().count() != pagos.size()) {
            throw new BusinessRuleException("Elegí hasta tres medios de pago diferentes");
        }
        BigDecimal suma = pagos.stream().map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(total) > 0) {
            throw new BusinessRuleException("Los pagos no pueden superar el total de la compra");
        }
        if (suma.compareTo(total) < 0 && clienteId == null) {
            throw new BusinessRuleException("Elegí un cliente para guardar el saldo pendiente");
        }
    }
}
