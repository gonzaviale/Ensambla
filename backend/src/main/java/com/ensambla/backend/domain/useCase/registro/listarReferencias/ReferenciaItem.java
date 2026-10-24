package com.ensambla.backend.domain.useCase.registro.listarReferencias;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Referencia;
import com.ensambla.backend.domain.model.registro.Registro;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record ReferenciaItem(UUID id, String etiqueta) {

    /** {@code campos} son los del bloque de los registros: de ahí sale la etiqueta de cada fila. */
    public static List<ReferenciaItem> de(List<Registro> registros, List<Campo> campos) {
        Function<Registro, Referencia> etiquetador = Referencia.etiquetador(campos);
        return registros.stream()
                .map(etiquetador)
                .map(referencia -> new ReferenciaItem(referencia.id(), referencia.etiqueta()))
                .toList();
    }
}
