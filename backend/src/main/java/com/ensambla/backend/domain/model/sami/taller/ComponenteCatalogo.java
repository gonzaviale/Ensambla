package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Un componente del editor que Sami puede sumar al sistema (inventario, ventas, tabla a medida…).
 *
 * @param tipo     identificador del frontend ({@code inventory}, {@code form}…)
 * @param singular cómo se llama una fila ({@code producto}, {@code cliente})
 * @param campos   los datos que trae de fábrica
 */
public record ComponenteCatalogo(String tipo, String nombre, String singular, String proposito,
                                 List<CampoComponente> campos) {

    public ComponenteCatalogo {
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
    }

    /** Sólo la tabla a medida puede estar más de una vez en un sistema (y por eso siempre lleva nombre). */
    public boolean repetible() {
        return CatalogoComponentes.TIPO_A_MEDIDA.equals(tipo);
    }

    /** Los datos de fábrica tal como los ve el taller. */
    public List<CampoTaller> camposTaller() {
        return campos.stream().map(campo -> new CampoTaller(campo.etiqueta(), campo.tipo())).toList();
    }

    /** Etiqueta del dato que apunta a este componente desde otro: el singular con mayúscula ({@code Cliente}). */
    public String etiquetaDeVinculo() {
        return singular.substring(0, 1).toUpperCase(Locale.ROOT) + singular.substring(1);
    }
}
