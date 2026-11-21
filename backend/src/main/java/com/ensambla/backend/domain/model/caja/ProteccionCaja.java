package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProteccionCaja {

    private static final String ORIGEN_DE_CAJA = "Caja ";
    private static final String VENTA_NUEVA = "Hubo una venta en Caja. Actualizá los datos antes de guardar";
    private static final String VENTA_MODIFICADA =
            "Las ventas confirmadas de Caja no se editan ni se eliminan por fila";

    private ProteccionCaja() {
    }

    public static void validar(List<Sistema> actuales, Sistema propuesto, OffsetDateTime leidoEn) {
        actuales.stream().filter(sistema -> sistema.id().equals(propuesto.id()))
                .forEach(actual -> validarVentasDe(actual, propuesto, leidoEn));
    }

    private static void validarVentasDe(Sistema actual, Sistema propuesto, OffsetDateTime leidoEn) {
        ventasDeCajaDe(actual).forEach(venta -> validarVenta(venta, actual, propuesto, leidoEn));
    }

    private static List<Registro> ventasDeCajaDe(Sistema sistema) {
        return sistema.registrosPorBloque().values().stream().flatMap(List::stream)
                .filter(ProteccionCaja::esDeCaja).toList();
    }

    private static boolean esDeCaja(Registro fila) {
        return fila.getOrigen() != null && fila.getOrigen().startsWith(ORIGEN_DE_CAJA);
    }

    private static void validarVenta(Registro venta, Sistema actual, Sistema propuesto, OffsetDateTime leidoEn) {
        exigirUnaLecturaPosteriorA(venta, leidoEn);
        exigirQueSeConserve(venta, actual, propuesto);
    }

    private static void exigirUnaLecturaPosteriorA(Registro venta, OffsetDateTime leidoEn) {
        if (leidoEn == null || venta.getCreadoEn().isAfter(leidoEn)) {
            throw new DuplicateResourceException(VENTA_NUEVA);
        }
    }

    private static void exigirQueSeConserve(Registro venta, Sistema actual, Sistema propuesto) {
        if (!seConserva(venta, actual, propuesto)) {
            throw new DuplicateResourceException(VENTA_MODIFICADA);
        }
    }

    private static boolean seConserva(Registro venta, Sistema actual, Sistema propuesto) {
        return propuesto.registrosPorBloque().getOrDefault(venta.getBloqueId(), List.of()).stream()
                .anyMatch(nueva -> esLaMismaVenta(venta, actual, nueva, propuesto));
    }

    private static boolean esLaMismaVenta(Registro venta, Sistema actual, Registro nueva, Sistema propuesto) {
        return venta.getId().equals(nueva.getId())
                && venta.getOrigen().equals(nueva.getOrigen())
                && valoresPorClave(venta, actual).equals(valoresPorClave(nueva, propuesto));
    }

    /**
     * Cada guardado desde la pantalla vuelve a calcular los ids de las columnas a partir de su clave, así que un
     * sistema armado con otros ids se leería distinto sin que nadie haya tocado la venta: se comparan por clave.
     */
    private static Map<String, Object> valoresPorClave(Registro fila, Sistema sistema) {
        Map<String, Object> valores = new LinkedHashMap<>(fila.getValores());
        sistema.columnasDe(fila.getBloqueId()).forEach(columna -> renombrarPorClave(valores, columna));
        return valores;
    }

    private static void renombrarPorClave(Map<String, Object> valores, Campo columna) {
        Object valor = valores.remove(columna.getId().toString());
        if (valor != null) {
            valores.put(columna.claveDePantalla(), valor);
        }
    }
}
