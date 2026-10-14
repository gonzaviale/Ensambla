package com.ensambla.backend.domain.model.common;

import java.text.Normalizer;
import java.util.Locale;

/** Compara textos escritos a mano sin que importen las tildes, las mayúsculas ni los espacios de los bordes. */
public final class TextoNormalizado {

    private TextoNormalizado() {
    }

    public static String de(String valor) {
        return Normalizer.normalize(valor.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
