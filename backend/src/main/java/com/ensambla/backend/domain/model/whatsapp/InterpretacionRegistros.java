package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.util.List;
import java.util.Map;

/**
 * Lo que la IA entendió de un texto.
 *
 * @param bloqueId  la tabla que corresponde; null si ninguna encaja
 * @param filas     el valor, tal como lo escribió la persona, de cada campo que la IA pudo completar
 * @param confianza de 0 a 1: qué tan segura está de la tabla y de los valores
 * @param consumo   lo que costó la consulta; null si no hubo llamada a un proveedor
 */
public record InterpretacionRegistros(BloqueId bloqueId, List<Map<CampoId, String>> filas, double confianza,
                                      ConsumoIaSami consumo) {

    public InterpretacionRegistros {
        filas = List.copyOf(filas);
    }

    public static InterpretacionRegistros sinResultado(ConsumoIaSami consumo) {
        return new InterpretacionRegistros(null, List.of(), 0.0, consumo);
    }

    public boolean entendida(double umbral) {
        return bloqueId != null && !filas.isEmpty() && confianza >= umbral;
    }
}
