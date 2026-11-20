package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.TextoNormalizado;
import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.ImagenCuaderno;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.PropuestaLectura;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Una foto del cuaderno: se descarta la carga abierta (si hay), se lee la foto, que descuenta del cupo de páginas del
 * plan, y se propone cargarla en la tabla que corresponde o se le pregunta a la persona cuál es.
 */
@RequiredArgsConstructor
final class ManejadorDeImagen implements ManejadorDeMensaje {

    private final TablasDelNegocio tablasDelNegocio;
    private final Conversacion conversacion;
    private final PropuestasDelChat propuestas;
    private final ControlPlanService controlPlanService;
    private final LectorCuadernoPort lectorCuadernoPort;
    private final FilasDeLaIa filasDeLaIa;
    private final EmparejamientoDeFilas emparejamiento;
    private final PreguntarTabla preguntarTabla;
    private final PoliticaAtencionWhatsapp politica;

    /** Lo que salió de leer la foto: lo que devolvió el lector y lo que se propone cargar a partir de eso. */
    private record FotoLeida(LecturaCuaderno lectura, PropuestaLectura propuesta) {
    }

    @Override
    public void atender(Atencion atencion, Optional<PropuestaWhatsapp> abierta) {
        abierta.ifPresent(
                propuesta -> propuestas.descartarAvisando(propuesta, atencion, TextosWhatsapp.DESCARTE_ANTERIOR));
        leerFoto(atencion);
    }

    private void leerFoto(Atencion atencion) {
        List<TablaDisponible> tablas = tablasDelNegocio.de(atencion.negocioId());
        ImagenCuaderno imagen = ImagenCuaderno.de(conversacion.descargarFoto(atencion.mensaje()).contenido());
        reservarLaPagina(atencion.negocioId(), atencion.ahora());
        conversacion.responder(atencion, TextosWhatsapp.LEYENDO_FOTO);
        FotoLeida foto = leer(imagen, tablas, atencion.texto());
        foto.propuesta().lectura().bloque().bloqueExistente().ifPresentOrElse(
                bloqueId -> proponer(atencion, TablasDelNegocio.tablaDe(tablas, bloqueId), tablas, foto.propuesta()),
                () -> preguntarTabla.preguntar(atencion, OrigenPropuestaWhatsapp.FOTO, foto.lectura(), tablas, null));
    }

    private void reservarLaPagina(NegocioId negocioId, OffsetDateTime ahora) {
        controlPlanService.reservarPaginaIaInmediata(negocioId, politica.limiteDiarioPaginas(), ahora);
    }

    private FotoLeida leer(ImagenCuaderno imagen, List<TablaDisponible> tablas, String pie) {
        List<BloqueLectura> bloques = TablasDelNegocio.bloquesDe(tablas);
        BloqueId destino = destinoDelPie(pie, tablas);
        LecturaCuaderno lectura = lectorCuadernoPort.leer(List.of(imagen.comoPagina()),
                conocidosPara(bloques, destino), destino);
        return new FotoLeida(lectura, PropuestaLectura.proponer(lectura, bloques, destino));
    }

    private static List<BloqueConocido> conocidosPara(List<BloqueLectura> bloques, BloqueId destino) {
        return bloques.stream()
                .filter(bloque -> destino == null || bloque.id().equals(destino))
                .map(bloque -> bloque.paraElLector(bloques))
                .toList();
    }

    /** Si el pie de la foto nombra una sola tabla, va a esa. */
    private static BloqueId destinoDelPie(String pie, List<TablaDisponible> tablas) {
        if (pie == null || pie.isBlank()) {
            return null;
        }
        List<TablaDisponible> nombradas = tablasNombradasEn(pie, tablas);
        return nombradas.size() == 1 ? nombradas.getFirst().bloque().id() : null;
    }

    private static List<TablaDisponible> tablasNombradasEn(String pie, List<TablaDisponible> tablas) {
        String buscado = TextoNormalizado.de(pie);
        return tablas.stream()
                .filter(tabla -> buscado.contains(TextoNormalizado.de(tabla.bloque().nombre())))
                .toList();
    }

    private void proponer(Atencion atencion, TablaDisponible tabla, List<TablaDisponible> tablas,
                          PropuestaLectura propuesta) {
        List<FilaPropuesta> filas = emparejamiento.armar(filasDeLaIa.deFoto(propuesta, tabla), tabla, tablas,
                atencion.negocioId());
        if (filas.isEmpty()) {
            conversacion.responder(atencion, TextosWhatsapp.NO_ENTENDI);
            return;
        }
        propuestas.proponer(atencion, OrigenPropuestaWhatsapp.FOTO, tabla, filas);
    }
}
