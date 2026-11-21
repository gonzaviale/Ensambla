package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

/** Le da cada mensaje al manejador de su tipo, después de vencer la carga abierta que ya pasó de hora. */
@RequiredArgsConstructor
final class DespachoDeMensaje {

    private final PropuestasDelChat propuestas;
    private final Conversacion conversacion;
    private final Map<TipoMensajeWhatsapp, ManejadorDeMensaje> manejadores;

    void despachar(Atencion atencion) {
        Optional<PropuestaWhatsapp> abierta = propuestas.abiertaDe(atencion.telefono());
        boolean seVencio = vencerLaQueYaPasoDeHora(abierta, atencion);
        ManejadorDeMensaje manejador = manejadorDe(atencion);
        if (seVencio && manejador.esUnaRespuesta(atencion)) {
            conversacion.responder(atencion, TextosWhatsapp.VENCIDA);
            return;
        }
        manejador.atender(atencion, abierta.filter(propuesta -> propuesta.estaVigente(atencion.ahora())));
    }

    private boolean vencerLaQueYaPasoDeHora(Optional<PropuestaWhatsapp> abierta, Atencion atencion) {
        Optional<PropuestaWhatsapp> vencida = abierta.filter(propuesta -> !propuesta.estaVigente(atencion.ahora()));
        vencida.ifPresent(propuesta -> propuestas.vencer(propuesta, atencion.ahora()));
        return vencida.isPresent();
    }

    private ManejadorDeMensaje manejadorDe(Atencion atencion) {
        return manejadores.get(atencion.mensaje().getTipo());
    }
}
