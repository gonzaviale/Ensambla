package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.common.TextoNormalizado;

import java.util.List;
import java.util.Set;

/**
 * Nombres que en un cuaderno suelen querer decir lo mismo («Tel», «Celular» y «Teléfono»). Se usan para no proponer
 * como campo nuevo una columna que el bloque ya tiene escrita de otra manera. La lista es corta a propósito: sólo
 * entran palabras que no se confunden con otra columna distinta.
 */
public final class SinonimosCampo {

    private static final List<Set<String>> GRUPOS = List.of(
            Set.of("telefono", "tel", "celular", "cel", "movil", "whatsapp", "wsp"),
            Set.of("direccion", "domicilio", "dir"),
            Set.of("monto", "importe", "total"),
            Set.of("cantidad", "cant", "unidades"),
            Set.of("producto", "articulo", "item"),
            Set.of("cliente", "clienta", "comprador"),
            Set.of("email", "mail", "correo", "correo electronico"),
            Set.of("observaciones", "observacion", "obs", "notas", "nota", "comentarios", "comentario"));

    private SinonimosCampo() {
    }

    /** Nombre sin tildes, mayúsculas, puntuación ni espacios de más: «Tel.» y « tel » dan lo mismo. */
    public static String limpio(String nombre) {
        return TextoNormalizado.de(nombre).replaceAll("[^a-z0-9]+", " ").strip();
    }

    public static boolean mismoNombre(String uno, String otro) {
        return limpio(uno).equals(limpio(otro));
    }

    public static boolean equivalentes(String uno, String otro) {
        String a = limpio(uno);
        String b = limpio(otro);
        return GRUPOS.stream().anyMatch(grupo -> grupo.contains(a) && grupo.contains(b));
    }
}
