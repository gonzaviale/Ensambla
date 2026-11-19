package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/** Cuando Sami no está segura de la tabla, le ofrece a la persona una lista numerada para que elija. */
@RequiredArgsConstructor
final class PreguntarTabla {

    private static final int MAX_OPCIONES = 9;

    private final PropuestasDelChat propuestas;
    private final Conversacion conversacion;

    /**
     * @param lectura  lo leído de la foto, para no volver a llamar a la IA; null si el origen es un texto
     * @param sugerida la tabla que Sami cree más probable, que va primera; null si no tiene ninguna
     */
    void preguntar(Atencion atencion, OrigenPropuestaWhatsapp origen, LecturaCuaderno lectura,
                   List<TablaDisponible> tablas, TablaDisponible sugerida) {
        List<TablaDisponible> ofrecidas = ofrecidas(tablas, sugerida);
        propuestas.guardar(PropuestaWhatsapp.eligiendoTabla(atencion.negocioId(), atencion.telefono(), origen,
                atencion.texto(), lectura, idsDe(ofrecidas), atencion.ahora()));
        conversacion.responder(atencion, TextosWhatsapp.elegirTabla(ofrecidas));
    }

    private static List<TablaDisponible> ofrecidas(List<TablaDisponible> tablas, TablaDisponible sugerida) {
        return Stream.concat(Stream.ofNullable(sugerida), tablas.stream().filter(tabla -> tabla != sugerida))
                .limit(MAX_OPCIONES)
                .toList();
    }

    private static List<BloqueId> idsDe(List<TablaDisponible> tablas) {
        return tablas.stream().map(tabla -> tabla.bloque().id()).toList();
    }
}
