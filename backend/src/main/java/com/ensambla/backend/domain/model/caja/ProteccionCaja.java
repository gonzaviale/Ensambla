package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.time.OffsetDateTime;
import java.util.List;

public final class ProteccionCaja {

    private ProteccionCaja() {
    }

    public static void validar(List<Sistema> actuales, Sistema propuesto, OffsetDateTime leidoEn) {
        actuales.stream().filter(sistema -> sistema.id().equals(propuesto.id()))
                .flatMap(sistema -> sistema.registrosPorBloque().values().stream()).flatMap(List::stream)
                .filter(fila -> fila.getOrigen() != null && fila.getOrigen().startsWith("Caja "))
                .forEach(fila -> validarFila(fila, propuesto, leidoEn));
    }

    private static void validarFila(Registro fila, Sistema propuesto, OffsetDateTime leidoEn) {
        if (leidoEn == null || fila.getCreadoEn().isAfter(leidoEn)) {
            throw new DuplicateResourceException("Hubo una venta en Caja. Actualizá los datos antes de guardar");
        }
        boolean conservada = propuesto.registrosPorBloque().getOrDefault(fila.getBloqueId(), List.of()).stream()
                .anyMatch(nueva -> fila.getId().equals(nueva.getId()) && fila.getValores().equals(nueva.getValores())
                        && fila.getOrigen().equals(nueva.getOrigen()));
        if (!conservada) {
            throw new DuplicateResourceException("Las ventas confirmadas de Caja no se editan ni se eliminan por fila");
        }
    }
}
