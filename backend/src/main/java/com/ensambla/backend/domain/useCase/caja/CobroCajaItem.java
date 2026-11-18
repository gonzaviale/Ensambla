package com.ensambla.backend.domain.useCase.caja;

import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CobroCajaItem(CobroCajaId id, RegistroId clienteId, String clienteNombre, List<AplicacionItem> aplicaciones,
                            BigDecimal importe, OffsetDateTime creadoEn) {

    public record AplicacionItem(VentaCajaId ventaId, List<PagoItem> pagos, BigDecimal importe) {
    }

    public record PagoItem(String medio, BigDecimal importe) {
    }

    public static CobroCajaItem de(CobroCaja cobro) {
        return new CobroCajaItem(cobro.id(), cobro.clienteId(), cobro.clienteNombre(), cobro.aplicaciones().stream().map(aplicacion ->
                new AplicacionItem(aplicacion.ventaId(), aplicacion.pagos().stream().map(pago ->
                        new PagoItem(pago.medio().name(), pago.importe())).toList(), aplicacion.importe())).toList(),
                cobro.importe(), cobro.creadoEn());
    }
}
