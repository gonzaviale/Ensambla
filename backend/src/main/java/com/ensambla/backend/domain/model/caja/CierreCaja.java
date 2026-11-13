package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CierreCaja(CierreCajaId id, NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
                         String huella, ResumenCaja resumen, BigDecimal fondoInicial, BigDecimal entradas,
                         BigDecimal retiros, BigDecimal esperado, BigDecimal contado, BigDecimal diferencia,
                         String nota, OffsetDateTime cerradoEn) {

    public static CierreCaja nuevo(CierreCajaId id, NegocioId negocioId, ProyectoId proyectoId,
            BloqueId inventarioId, String huella, ResumenCaja resumen, String revision,
            BigDecimal fondoInicial, BigDecimal entradas, BigDecimal retiros, BigDecimal contado,
            String nota, OffsetDateTime ahora) {
        if (resumen.ventas().isEmpty()) {
            throw new BusinessRuleException("No hay ventas pendientes de cierre");
        }
        if (!resumen.revision().equals(revision)) {
            throw new DuplicateResourceException("Entraron nuevas ventas. Revisá el resumen actualizado antes de cerrar");
        }
        validarDinero(fondoInicial);
        validarDinero(entradas);
        validarDinero(retiros);
        BigDecimal esperado = fondoInicial.add(resumen.efectivo()).add(entradas).subtract(retiros);
        if (esperado.signum() < 0) {
            throw new BusinessRuleException("Los retiros superan el efectivo disponible");
        }
        BigDecimal diferencia = null;
        if (contado != null) {
            validarDinero(contado);
            diferencia = contado.subtract(esperado);
        }
        return new CierreCaja(id, negocioId, proyectoId, inventarioId, huella, resumen, fondoInicial,
                entradas, retiros, esperado, contado, diferencia, nota, ahora);
    }

    private static void validarDinero(BigDecimal importe) {
        if (importe.signum() < 0 || importe.scale() > 2) {
            throw new BusinessRuleException("Usá importes positivos o cero, con hasta dos decimales");
        }
    }
}
