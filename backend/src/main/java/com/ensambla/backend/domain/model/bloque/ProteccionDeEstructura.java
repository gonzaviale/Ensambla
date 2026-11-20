package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.sistema.Sistema;

import java.util.List;
import java.util.Optional;

/**
 * Que un guardado no quite ni cambie lo que Ensambla necesita para funcionar: la última sección de cada tipo
 * obligatorio, el tipo de esas secciones y sus columnas fijas. Compara el sistema guardado con el propuesto; lo que
 * nunca existió no se rechaza, lo completa {@link EstructuraFija}.
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
        TipoBloque.obligatorios().forEach(tipo -> exigirQueSigaHabiendoUna(tipo, actual, propuesto));
        actual.proyecto().getBloques().forEach(bloque -> exigirQueConserve(bloque, actual, propuesto));
    }

    private static void exigirQueSigaHabiendoUna(TipoBloque tipo, Sistema actual, Sistema propuesto) {
        if (haySeccionDe(tipo, actual) && !haySeccionDe(tipo, propuesto)) {
            throw rechazo(tipo.mensajeAlQuitarLaUltima());
        }
    }

    private static boolean haySeccionDe(TipoBloque tipo, Sistema sistema) {
        return sistema.proyecto().getBloques().stream().anyMatch(bloque -> bloque.getTipo() == tipo);
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
        Campo ahora = nuevo.getCampos().stream().filter(columna::esLaDe).findFirst()
                .orElseThrow(() -> rechazo(columna.mensajeAlQuitar(antes.getNombre())));
        if (!columna.laCumple(ahora, bloquesPropuestos)) {
            throw rechazo(columna.mensajeAlCambiar(antes.getNombre()));
        }
    }

    private static DuplicateResourceException rechazo(String mensaje) {
        return new DuplicateResourceException(mensaje);
    }
}
