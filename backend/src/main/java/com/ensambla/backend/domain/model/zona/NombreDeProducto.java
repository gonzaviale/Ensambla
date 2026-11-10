package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.List;
import java.util.Set;

/** Cuándo el producto de un negocio y el de la zona son el mismo, aunque se escriban distinto (tamaño, marca, plural). */
public final class NombreDeProducto {

    private static final int LARGO_MINIMO_DE_PALABRA = 3;

    /** Palabras que no distinguen un producto de otro: artículos, preposiciones, unidades y presentaciones. */
    private static final Set<String> VACIAS = Set.of("del", "las", "los", "con", "sin", "por", "para", "lts", "unidad",
            "unidades", "pack", "docena", "docen");

    private NombreDeProducto() {
    }

    /** Es el mismo producto si el nombre del negocio empieza por una palabra que también está en el de la zona. */
    public static boolean esElMismo(String nombreDelNegocio, String nombreDeLaZona) {
        List<String> tuyas = palabrasSignificativas(nombreDelNegocio);
        return !tuyas.isEmpty() && palabrasSignificativas(nombreDeLaZona).contains(tuyas.getFirst());
    }

    /** Sin tildes ni plurales, sin las palabras vacías, sin números y sin palabras de menos de tres letras. */
    private static List<String> palabrasSignificativas(String nombre) {
        return TextoNormalizado.palabras(nombre).stream()
                .filter(palabra -> palabra.length() >= LARGO_MINIMO_DE_PALABRA)
                .filter(palabra -> !VACIAS.contains(palabra))
                .filter(palabra -> !Character.isDigit(palabra.charAt(0)))
                .toList();
    }
}
