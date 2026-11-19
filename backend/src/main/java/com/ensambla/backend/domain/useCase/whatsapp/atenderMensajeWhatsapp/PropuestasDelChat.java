package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Las cargas que Sami armó y que la persona todavía no confirmó: cómo se guardan, se vencen y se descartan. */
@RequiredArgsConstructor
final class PropuestasDelChat {

    private final PropuestaWhatsappRepository propuestaRepository;
    private final Conversacion conversacion;

    Optional<PropuestaWhatsapp> abiertaDe(String telefono) {
        return propuestaRepository.findAbiertaByTelefono(telefono);
    }

    PropuestaWhatsapp guardar(PropuestaWhatsapp propuesta) {
        return propuestaRepository.save(propuesta);
    }

    void vencer(PropuestaWhatsapp propuesta, OffsetDateTime ahora) {
        propuesta.vencer(ahora);
        guardar(propuesta);
    }

    void descartarAvisando(PropuestaWhatsapp propuesta, Atencion atencion, String aviso) {
        propuesta.descartar(atencion.ahora());
        guardar(propuesta);
        conversacion.responder(atencion, aviso);
    }

    void proponer(Atencion atencion, OrigenPropuestaWhatsapp origen, TablaDisponible tabla,
                  List<FilaPropuesta> filas) {
        PropuestaWhatsapp propuesta = guardar(PropuestaWhatsapp.pendiente(atencion.negocioId(), atencion.telefono(),
                origen, atencion.texto(), tabla.bloque().id(), filas, atencion.ahora()));
        conversacion.responder(atencion, TextosWhatsapp.resumen(propuesta, tabla));
    }
}
