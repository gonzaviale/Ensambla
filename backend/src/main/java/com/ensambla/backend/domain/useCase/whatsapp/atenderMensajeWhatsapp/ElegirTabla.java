package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.RespuestaChat;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/** La persona contestó con el número de una tabla de la lista: con esa tabla se arma la carga y se espera el sí. */
@RequiredArgsConstructor
final class ElegirTabla implements ReaccionAUnaPropuesta {

    private final PropuestasDelChat propuestas;
    private final Conversacion conversacion;
    private final TablasDelNegocio tablasDelNegocio;
    private final EmparejamientoDeFilas emparejamiento;
    private final Map<OrigenPropuestaWhatsapp, ReinterpretarPropuesta> reinterpretaciones;
    private final ReaccionAUnaPropuesta sinListaPendiente;

    @Override
    public void ante(PropuestaWhatsapp propuesta, Atencion atencion) {
        if (!propuesta.estaEligiendoTabla()) {
            sinListaPendiente.ante(propuesta, atencion);
            return;
        }
        BloqueId destino = propuesta.elegir(RespuestaChat.numero(atencion.texto()));
        List<TablaDisponible> tablas = tablasDelNegocio.de(atencion.negocioId());
        TablaDisponible tabla = TablasDelNegocio.tablaDe(tablas, destino);
        List<FilaPropuesta> filas = armarFilas(propuesta, destino, tabla, tablas, atencion);
        proponerOCancelar(propuesta, destino, tabla, filas, atencion);
    }

    private List<FilaPropuesta> armarFilas(PropuestaWhatsapp propuesta, BloqueId destino, TablaDisponible tabla,
                                           List<TablaDisponible> tablas, Atencion atencion) {
        List<FilaPropuesta> nuevas = reinterpretaciones.get(propuesta.getOrigen())
                .filasPara(propuesta, destino, tabla, tablas, atencion);
        return emparejamiento.armar(nuevas, tabla, tablas, atencion.negocioId());
    }

    private void proponerOCancelar(PropuestaWhatsapp propuesta, BloqueId destino, TablaDisponible tabla,
                                   List<FilaPropuesta> filas, Atencion atencion) {
        if (filas.isEmpty()) {
            propuestas.descartarAvisando(propuesta, atencion, TextosWhatsapp.NO_ENTENDI);
            return;
        }
        propuesta.aPendiente(destino, filas);
        conversacion.responder(atencion, TextosWhatsapp.resumen(propuestas.guardar(propuesta), tabla));
    }
}
