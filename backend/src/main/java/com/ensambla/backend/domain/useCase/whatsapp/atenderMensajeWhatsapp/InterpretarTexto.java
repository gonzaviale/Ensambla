package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Un texto que no contesta a nada: Sami lo interpreta y propone filas, o pregunta la tabla si no está segura. */
@RequiredArgsConstructor
final class InterpretarTexto {

    private final TablasDelNegocio tablasDelNegocio;
    private final ConsultaASami consulta;
    private final FilasDeLaIa filasDeLaIa;
    private final EmparejamientoDeFilas emparejamiento;
    private final PropuestasDelChat propuestas;
    private final PreguntarTabla preguntarTabla;
    private final Conversacion conversacion;
    private final PoliticaAtencionWhatsapp politica;

    void interpretar(Atencion atencion) {
        List<TablaDisponible> tablas = tablasDelNegocio.de(atencion.negocioId());
        InterpretacionRegistros interpretacion = consulta.interpretar(atencion.negocioId(), atencion.texto(),
                tablas, null, atencion.ahora());
        if (interpretacion.bloqueId() == null) {
            avisarQueNoEntendio(atencion);
            return;
        }
        TablaDisponible tabla = TablasDelNegocio.tablaDe(tablas, interpretacion.bloqueId());
        List<FilaPropuesta> filas = emparejamiento.armar(filasDeLaIa.deTexto(interpretacion, tabla), tabla, tablas,
                atencion.negocioId());
        proponerOPreguntar(atencion, interpretacion, tabla, tablas, filas);
    }

    private void proponerOPreguntar(Atencion atencion, InterpretacionRegistros interpretacion, TablaDisponible tabla,
                                    List<TablaDisponible> tablas, List<FilaPropuesta> filas) {
        if (filas.isEmpty()) {
            avisarQueNoEntendio(atencion);
            return;
        }
        if (interpretacion.entendida(politica.umbralConfianza())) {
            propuestas.proponer(atencion, OrigenPropuestaWhatsapp.TEXTO, tabla, filas);
            return;
        }
        preguntarTabla.preguntar(atencion, OrigenPropuestaWhatsapp.TEXTO, null, tablas, tabla);
    }

    private void avisarQueNoEntendio(Atencion atencion) {
        conversacion.responder(atencion, TextosWhatsapp.NO_ENTENDI);
    }
}
