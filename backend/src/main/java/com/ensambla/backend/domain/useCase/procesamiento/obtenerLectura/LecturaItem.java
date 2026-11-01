package com.ensambla.backend.domain.useCase.procesamiento.obtenerLectura;

import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;

public record LecturaItem(LoteId loteId, String estado, String motivoError, int intentos, PropuestaItem propuesta) {

    public static LecturaItem de(EstadoLectura estado) {
        return new LecturaItem(estado.id(), estado.estado().name(), estado.motivoError(), estado.intentos(),
                PropuestaItem.de(estado.propuesta()));
    }
}
