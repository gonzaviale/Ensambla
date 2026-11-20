package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.aviso.VentaProducto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Lo que entró en un día por las ventas con precio; sirve para dibujar las ventas día por día. */
public record VentaDelDia(LocalDate fecha, BigDecimal ingresos) {

    /** Un punto por cada día entre {@code desde} y {@code hasta}, ambos incluidos; los días sin ventas valen cero. */
    public static List<VentaDelDia> serie(List<VentaProducto> ventas, LocalDate desde, LocalDate hasta) {
        List<VentaDelDia> serie = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            LocalDate fecha = dia;
            BigDecimal ingresos = ventas.stream()
                    .filter(venta -> venta.fecha().equals(fecha) && venta.precio() != null)
                    .map(venta -> venta.precio().multiply(venta.cantidad()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            serie.add(new VentaDelDia(fecha, ingresos));
        }
        return serie;
    }
}
