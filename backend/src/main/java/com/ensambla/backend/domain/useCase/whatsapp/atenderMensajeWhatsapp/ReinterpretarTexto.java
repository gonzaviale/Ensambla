package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** El texto original se le vuelve a pasar a Sami, ahora con la tabla que eligió la persona. */
@RequiredArgsConstructor
final class ReinterpretarTexto implements ReinterpretarPropuesta {

    private final ConsultaASami consulta;
    private final FilasDeLaIa filasDeLaIa;

    @Override
    public List<FilaPropuesta> filasPara(PropuestaWhatsapp propuesta, BloqueId destino, TablaDisponible tabla,
                                         List<TablaDisponible> tablas, Atencion atencion) {
        InterpretacionRegistros interpretacion = consulta.interpretar(atencion.negocioId(), propuesta.getTexto(),
                tablas, destino, atencion.ahora());
        return filasDeLaIa.deTexto(interpretacion, tabla);
    }
}
