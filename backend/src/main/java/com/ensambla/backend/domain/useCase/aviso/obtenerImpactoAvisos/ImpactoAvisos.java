package com.ensambla.backend.domain.useCase.aviso.obtenerImpactoAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que el negocio hizo con los avisos y qué pasó con sus ventas después de aplicarlos.
 *
 * @param aplicados              avisos que la persona aplicó
 * @param descartados            avisos que la persona descartó
 * @param ingresoSemanalDelta    suma de lo que cambió el ingreso semanal en los avisos ya medidos; es una comparación
 *                               entre antes y después, no la parte atribuible al aviso
 * @param medidos                avisos aplicados cuyo impacto ya se midió, del que más cambió al que menos
 */
public record ImpactoAvisos(
        long aplicados,
        long descartados,
        BigDecimal ingresoSemanalDelta,
        List<ImpactoDeAviso> medidos) {

    public static ImpactoAvisos de(List<Aviso> avisos) {
        List<ImpactoDeAviso> medidos = avisos.stream()
                .flatMap(aviso -> ImpactoDeAviso.de(aviso).stream())
                .sorted((a, b) -> b.impacto().deltaIngresoSemanal().compareTo(a.impacto().deltaIngresoSemanal()))
                .toList();
        BigDecimal total = medidos.stream()
                .map(medido -> medido.impacto().deltaIngresoSemanal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ImpactoAvisos(
                cantidad(avisos, EstadoAviso.APLICADO), cantidad(avisos, EstadoAviso.DESCARTADO), total, medidos);
    }

    private static long cantidad(List<Aviso> avisos, EstadoAviso estado) {
        return avisos.stream().filter(aviso -> aviso.getEstado() == estado).count();
    }
}
