package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;

import java.util.List;
import java.util.UUID;

/**
 * Estructura que Sami propone: la plantilla base y las columnas que le suma.
 *
 * @param origen ALGORITMO (diccionario y preguntas, sin costo) o IA
 */
public record PropuestaSamiItem(
        UUID plantillaId,
        String plantilla,
        List<ColumnaSamiItem> columnas,
        double confianza,
        String origen
) {

    public static final String ORIGEN_ALGORITMO = "ALGORITMO";
    public static final String ORIGEN_IA = "IA";

    static PropuestaSamiItem de(InterpretacionSami interpretacion, String origen) {
        return new PropuestaSamiItem(
                interpretacion.plantillaId(),
                interpretacion.plantilla(),
                interpretacion.columnasExtra().stream().map(ColumnaSamiItem::de).toList(),
                interpretacion.confianza(),
                origen);
    }
}
