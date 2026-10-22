package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos del negocio sobre los que las reglas de aviso trabajan en una corrida del motor: sus tablas de inventario y de
 * ventas, la zona a la que pertenece y el día de hoy (en hora argentina). Las reglas que necesitan {@code hoy} no
 * devuelven nada si es {@code null}.
 */
public record ContextoNegocio(
        UUID negocioId,
        LocalDate hoy,
        List<BloqueInventario> inventarios,
        List<BloqueVentas> ventas,
        AnalisisZona zona) {

    /** Contexto sólo con inventarios: sin fecha, sin ventas y sin zona. */
    public ContextoNegocio(UUID negocioId, List<BloqueInventario> inventarios) {
        this(negocioId, null, inventarios, List.of(), AnalisisZona.sinZona());
    }
}
