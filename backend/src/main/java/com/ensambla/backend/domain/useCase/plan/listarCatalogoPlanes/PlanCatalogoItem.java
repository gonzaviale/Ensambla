package com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes;

import com.ensambla.backend.domain.model.plan.Plan;

import java.math.BigDecimal;
import java.util.List;

public record PlanCatalogoItem(String codigo, String nombre, boolean precioConfigurado, boolean limitesConfigurados,
                               BigDecimal precioMensual, String moneda, Integer limiteProyectos,
                               Long limiteRegistros, Integer limitePaginasIaMensuales, List<String> funciones) {

    public static PlanCatalogoItem de(Plan plan) {
        return new PlanCatalogoItem(plan.getCodigo().name(), plan.getNombre(),
                Boolean.TRUE.equals(plan.getConfigurado()), Boolean.TRUE.equals(plan.getLimitesConfigurados()),
                plan.getPrecioMensual(), plan.getMoneda(), plan.getLimiteProyectos(), plan.getLimiteRegistros(),
                plan.getLimitePaginasIaMensuales(), plan.getFunciones());
    }
}
