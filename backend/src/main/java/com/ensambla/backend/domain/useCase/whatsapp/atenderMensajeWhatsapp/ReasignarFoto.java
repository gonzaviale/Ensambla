package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Lo que ya se leyó de la foto se asigna a la tabla elegida, sin volver a llamar a la IA. */
@RequiredArgsConstructor
final class ReasignarFoto implements ReinterpretarPropuesta {

    private final FilasDeLaIa filasDeLaIa;

    @Override
    public List<FilaPropuesta> filasPara(PropuestaWhatsapp propuesta, BloqueId destino, TablaDisponible tabla,
                                         List<TablaDisponible> tablas, Atencion atencion) {
        PropuestaLectura lectura = PropuestaLectura.proponer(propuesta.getLectura(),
                TablasDelNegocio.bloquesDe(tablas), destino);
        return filasDeLaIa.deFoto(lectura, tabla);
    }
}
