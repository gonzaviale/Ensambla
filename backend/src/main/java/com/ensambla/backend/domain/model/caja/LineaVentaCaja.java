package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LineaVentaCaja(RegistroId productoId, String nombre, ModoVenta modo, BigDecimal cantidad,
                             BigDecimal precio, BigDecimal importe) {

    public static LineaVentaCaja de(RegistroId productoId, String nombre, ModoVenta modo,
            BigDecimal cantidad, BigDecimal precio) {
        if (cantidad.signum() <= 0 || cantidad.scale() > 3 || precio.signum() <= 0 || precio.scale() > 2) {
            throw new BusinessRuleException("Revisá la cantidad y el precio del artículo");
        }
        if (modo == ModoVenta.UNIDAD && cantidad.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("La cantidad por unidad debe ser entera");
        }
        BigDecimal importe = cantidad.multiply(precio).setScale(2, RoundingMode.HALF_UP);
        if (importe.signum() <= 0) {
            throw new BusinessRuleException("El importe del artículo debe ser mayor a cero");
        }
        return new LineaVentaCaja(productoId, nombre, modo, cantidad, precio, importe);
    }
}
