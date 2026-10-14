package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Cómo se muestra una fila cuando otra tabla la referencia con un campo RELACION: su id y una etiqueta legible.
 * La etiqueta es el valor del primer campo de texto u opción del bloque (por posición); si el bloque no tiene campos
 * de esos tipos, el del primer campo; si la fila no cargó ese dato, "Sin nombre".
 */
public record Referencia(UUID id, String etiqueta) {

    public static final String SIN_NOMBRE = "Sin nombre";

    private static final Comparator<Campo> POR_POSICION = Comparator.comparing(Campo::getPosicion)
            .thenComparing(Campo::getId);

    public static Referencia de(Registro registro, List<Campo> campos) {
        return etiquetador(campos).apply(registro);
    }

    /** Elige una sola vez el campo que da nombre a las filas del bloque y devuelve cómo armar cada referencia. */
    public static Function<Registro, Referencia> etiquetador(List<Campo> campos) {
        Optional<String> clave = campoEtiqueta(campos).map(campo -> campo.getId().toString());
        return registro -> new Referencia(registro.getId(), etiquetaDe(registro, clave));
    }

    private static Optional<Campo> campoEtiqueta(List<Campo> campos) {
        List<Campo> ordenados = campos.stream().sorted(POR_POSICION).toList();
        return ordenados.stream()
                .filter(Referencia::esDescriptivo)
                .findFirst()
                .or(() -> ordenados.stream().findFirst());
    }

    private static boolean esDescriptivo(Campo campo) {
        return campo.getTipo() == TipoCampo.TEXTO || campo.getTipo() == TipoCampo.OPCION;
    }

    private static String etiquetaDe(Registro registro, Optional<String> clave) {
        return clave.map(registro.getValores()::get)
                .map(String::valueOf)
                .filter(texto -> !texto.isBlank())
                .orElse(SIN_NOMBRE);
    }
}
