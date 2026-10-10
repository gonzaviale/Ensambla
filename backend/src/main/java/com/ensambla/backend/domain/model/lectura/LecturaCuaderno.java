package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.enums.TipoRegistroCuaderno;

import java.util.List;

/** Resultado validado de leer un lote de páginas: estructura propuesta + filas + avisos, sin guardar nada todavía. */
public record LecturaCuaderno(TipoRegistroCuaderno tipoRegistro, BloquePropuesto bloque, List<FilaLeida> filas,
                              List<String> advertencias, ConsumoIa consumo) {

    public LecturaCuaderno {
        filas = List.copyOf(filas);
        advertencias = List.copyOf(advertencias);
    }
}
