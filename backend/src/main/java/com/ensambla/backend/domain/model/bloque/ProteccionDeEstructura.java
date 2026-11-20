package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.util.List;
import java.util.Optional;

/**
 * Que un guardado no cambie lo que Ensambla necesita para funcionar: el tipo de las secciones que llevan columnas
 * fijas y el tipo de dato, la obligatoriedad y el destino de esas columnas. Compara el sistema guardado con el
 * propuesto. Lo que falta no se rechaza: lo repone {@link EstructuraFija}, porque quien guarda puede no haber recibido
 * todavía lo que el servidor completó (por ejemplo, un negocio nuevo que se guarda dos veces).
 */
public final class ProteccionDeEstructura {

    private ProteccionDeEstructura() {
    }

    public static void validar(List<Sistema> actuales, Sistema propuesto) {
        actuales.stream().filter(sistema -> sistema.id().equals(propuesto.id()))
                .forEach(actual -> validar(actual, propuesto));
    }

    /** Para quitar una sección por fuera del guardado del negocio entero. */
    public static void exigirQueSePuedaQuitarLaSeccion(Bloque bloque, List<Bloque> bloquesDelSistema) {
        if (!EstructuraFija.sePuedeQuitar(bloque, bloquesDelSistema)) {
            throw rechazo(bloque.getTipo().mensajeAlQuitarLaUltima());
        }
    }

    /** Para quitar una columna por fuera del guardado del negocio entero. */
    public static void exigirQueSePuedaQuitarLaColumna(Bloque bloque, Campo campo) {
        bloque.getTipo().columnasFijas().stream().filter(columna -> columna.esLaDe(campo)).findFirst()
                .ifPresent(columna -> {
                    throw rechazo(columna.mensajeAlQuitar(campo.getNombre()));
                });
    }

    /** Para cambiar una columna por fuera del guardado del negocio entero. */
    public static void exigirQueSePuedaCambiarLaColumna(Bloque bloque, Campo actual, Campo editado,
                                                        List<Bloque> bloquesDelSistema) {
        bloque.getTipo().columnasFijas().stream()
                .filter(columna -> columna.esLaDe(actual) && !columna.laCumple(editado, bloquesDelSistema))
                .findFirst().ifPresent(columna -> {
                    throw rechazo(columna.mensajeAlCambiar(actual.getNombre()));
                });
    }

    private static void validar(Sistema actual, Sistema propuesto) {
        actual.proyecto().getBloques().forEach(bloque -> exigirQueConserve(bloque, actual, propuesto));
    }

    private static void exigirQueConserve(Bloque actual, Sistema sistemaActual, Sistema propuesto) {
        buscarEn(propuesto, actual).ifPresent(nuevo -> {
            exigirQueNoCambieDeTipo(actual, nuevo);
            exigirQueConserveSusColumnas(actual, nuevo, sistemaActual, propuesto);
        });
    }

    private static Optional<Bloque> buscarEn(Sistema sistema, Bloque bloque) {
        return sistema.proyecto().getBloques().stream()
                .filter(candidato -> candidato.getId().equals(bloque.getId())).findFirst();
    }

    private static void exigirQueNoCambieDeTipo(Bloque actual, Bloque nuevo) {
        if (actual.getTipo().tieneColumnasFijas() && actual.getTipo() != nuevo.getTipo()) {
            throw rechazo(actual.getTipo().mensajeAlCambiarElTipo());
        }
    }

    private static void exigirQueConserveSusColumnas(Bloque actual, Bloque nuevo, Sistema sistemaActual,
                                                     Sistema propuesto) {
        List<Bloque> bloquesActuales = sistemaActual.proyecto().getBloques();
        List<Bloque> bloquesPropuestos = propuesto.proyecto().getBloques();
        actual.getTipo().columnasFijas().forEach(columna -> columnaQueCumplia(columna, actual, bloquesActuales)
                .ifPresent(antes -> exigirQueSigaCumpliendo(columna, antes, nuevo, bloquesPropuestos)));
    }

    private static Optional<Campo> columnaQueCumplia(ColumnaFija columna, Bloque bloque, List<Bloque> bloques) {
        return bloque.getCampos().stream()
                .filter(campo -> columna.esLaDe(campo) && columna.laCumple(campo, bloques)).findFirst();
    }

    private static void exigirQueSigaCumpliendo(ColumnaFija columna, Campo antes, Bloque nuevo,
                                                List<Bloque> bloquesPropuestos) {
        nuevo.getCampos().stream().filter(columna::esLaDe).findFirst()
                .filter(ahora -> !columna.laCumple(ahora, bloquesPropuestos))
                .ifPresent(ahora -> {
                    throw rechazo(columna.mensajeAlCambiar(antes.getNombre()));
                });
    }

    private static DuplicateResourceException rechazo(String mensaje) {
        return new DuplicateResourceException(mensaje);
    }
}
