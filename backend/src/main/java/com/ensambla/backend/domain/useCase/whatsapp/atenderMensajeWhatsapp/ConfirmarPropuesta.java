package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.TransaccionPort;
import lombok.RequiredArgsConstructor;

/** La persona dijo que sí: la carga se guarda de una sola vez y se le cuenta qué quedó. */
@RequiredArgsConstructor
final class ConfirmarPropuesta implements ReaccionAUnaPropuesta {

    private final TransaccionPort transaccionPort;
    private final GuardadoDePropuesta guardadoDePropuesta;
    private final Conversacion conversacion;

    @Override
    public void ante(PropuestaWhatsapp propuesta, Atencion atencion) {
        ResultadoDelGuardado guardado = guardarEnUnaTransaccion(propuesta, atencion);
        conversacion.responder(atencion, TextosWhatsapp.guardado(guardado.nuevas(), guardado.actualizadas(),
                guardado.tabla(), guardado.sumados(), guardado.stock()));
    }

    private ResultadoDelGuardado guardarEnUnaTransaccion(PropuestaWhatsapp propuesta, Atencion atencion) {
        return transaccionPort.ejecutar(
                () -> guardadoDePropuesta.guardar(propuesta, atencion.negocioId(), atencion.ahora()));
    }
}
