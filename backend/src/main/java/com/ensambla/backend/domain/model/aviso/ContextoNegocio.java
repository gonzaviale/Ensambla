package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos del negocio sobre los que las reglas de aviso trabajan en una corrida del motor: sus tablas de inventario y de
 * ventas, la zona a la que pertenece y el día de hoy (en hora argentina). Las reglas que necesitan {@code hoy} no
 * devuelven nada si es {@code null}.
 */
public record ContextoNegocio(
        NegocioId negocioId,
        LocalDate hoy,
        List<BloqueInventario> inventarios,
        List<BloqueVentas> ventas,
        AnalisisZona zona) {

    /** Contexto sólo con inventarios: sin fecha, sin ventas y sin zona. */
    public ContextoNegocio(NegocioId negocioId, List<BloqueInventario> inventarios) {
        this(negocioId, null, inventarios, List.of(), AnalisisZona.sinZona());
    }
}
