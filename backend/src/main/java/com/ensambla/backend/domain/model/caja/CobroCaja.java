package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CobroCaja(CobroCajaId id, NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
                        RegistroId clienteId, String clienteNombre, String huella,
                        List<Aplicacion> aplicaciones, OffsetDateTime creadoEn) {

    public CobroCaja {
        aplicaciones = List.copyOf(aplicaciones);
        if (aplicaciones.isEmpty() || aplicaciones.stream().map(Aplicacion::ventaId).distinct().count()
                != aplicaciones.size()) {
            throw new BusinessRuleException("Elegí ventas distintas para registrar el cobro");
        }
    }

    public BigDecimal importe() {
        return aplicaciones.stream().map(Aplicacion::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal importe(MedioPagoCaja medio) {
        return aplicaciones.stream().flatMap(aplicacion -> aplicacion.pagos().stream())
                .filter(pago -> pago.medio() == medio).map(PagoCaja::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Aplicacion(VentaCajaId ventaId, List<PagoCaja> pagos) {

        public Aplicacion {
            pagos = List.copyOf(pagos);
            if (pagos.isEmpty() || pagos.size() > 3
                    || pagos.stream().map(PagoCaja::medio).distinct().count() != pagos.size()) {
                throw new BusinessRuleException("Cada venta necesita un importe y medios de pago distintos");
            }
        }

        public BigDecimal importe() {
            return pagos.stream().map(PagoCaja::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
