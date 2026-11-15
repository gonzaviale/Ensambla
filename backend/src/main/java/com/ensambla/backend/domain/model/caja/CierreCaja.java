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
        exigirPeriodoActualizado(resumen, revision);
        BigDecimal esperado = efectivoEsperado(resumen, fondoInicial, entradas, retiros);
        exigirRecuento(contado);
        BigDecimal diferencia = contado.subtract(esperado);
        exigirMotivoDeDiferencia(diferencia, nota);
        return new CierreCaja(id, negocioId, proyectoId, inventarioId, huella, resumen, fondoInicial,
                entradas, retiros, esperado, contado, diferencia, nota.strip(), ahora);
    }

    private static void exigirPeriodoActualizado(ResumenCaja resumen, String revision) {
        if (resumen.ventas().isEmpty() && resumen.cobros().isEmpty()) {
            throw new BusinessRuleException("No hay ventas pendientes de cierre");
        }
        if (!resumen.revision().equals(revision)) {
            throw new DuplicateResourceException("Entraron nuevas ventas. Revisá el resumen actualizado antes de cerrar");
        }
    }

    private static BigDecimal efectivoEsperado(ResumenCaja resumen, BigDecimal fondo,
            BigDecimal entradas, BigDecimal retiros) {
        validarDinero(fondo);
        validarDinero(entradas);
        validarDinero(retiros);
        BigDecimal esperado = fondo.add(resumen.efectivo()).add(entradas).subtract(retiros);
        if (esperado.signum() < 0) {
            throw new BusinessRuleException("Los retiros superan el efectivo disponible");
        }
        return esperado;
    }

    private static void exigirRecuento(BigDecimal contado) {
        if (contado == null) {
            throw new BusinessRuleException("Contá el efectivo antes de cerrar. Si no hay efectivo, escribí 0");
        }
        validarDinero(contado);
    }

    private static void exigirMotivoDeDiferencia(BigDecimal diferencia, String nota) {
        if (diferencia.signum() != 0 && nota.isBlank()) {
            throw new BusinessRuleException("Explicá la diferencia entre el efectivo esperado y el contado");
        }
    }

    private static void validarDinero(BigDecimal importe) {
        if (importe.signum() < 0 || importe.scale() > 2) {
            throw new BusinessRuleException("Usá importes positivos o cero, con hasta dos decimales");
        }
    }
}
