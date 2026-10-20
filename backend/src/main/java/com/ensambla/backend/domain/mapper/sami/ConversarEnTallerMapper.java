package com.ensambla.backend.domain.mapper.sami;

import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse.AccionTallerItem;
import com.ensambla.backend.domain.dto.sami.ConversarEnTallerResponse.OpcionesTallerItem;
import com.ensambla.backend.domain.dto.sami.EstadoTallerItem;
import com.ensambla.backend.domain.dto.sami.ImagenTallerItem;
import com.ensambla.backend.domain.dto.sami.MensajeTallerItem;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;

import java.util.List;

public class ConversarEnTallerMapper {

    /** {@code null} si no adjuntó foto; si la foto no vale, {@link ImagenTaller} la rechaza. */
    public ImagenTaller toImagen(ImagenTallerItem imagen) {
        return imagen == null ? null : new ImagenTaller(imagen.tipo(), imagen.datos());
    }

    public EstadoTaller toEstado(EstadoTallerItem estado) {
        List<PiezaTaller> piezas = estado.piezas().stream()
                .map(pieza -> new PiezaTaller(
                        pieza.id(),
                        pieza.tipo(),
                        pieza.nombre(),
                        pieza.seccion(),
                        pieza.campos().stream()
                                .map(campo -> new CampoTaller(campo.etiqueta(), campo.tipo()))
                                .toList()))
                .toList();
        return new EstadoTaller(estado.nombre(), estado.distribucion(), piezas);
    }

    public List<MensajeTaller> toHistorial(List<MensajeTallerItem> historial) {
        return historial.stream().map(mensaje -> new MensajeTaller(mensaje.quien(), mensaje.texto())).toList();
    }

    public ConversarEnTallerResponse toResponse(RespuestaTaller respuesta) {
        List<AccionTallerItem> acciones = respuesta.acciones().stream()
                .map(accion -> new AccionTallerItem(accion.herramienta(), accion.argumentos()))
                .toList();
        List<OpcionesTallerItem> preguntas = respuesta.preguntas().stream()
                .map(duda -> new OpcionesTallerItem(duda.tema(), duda.pregunta(), duda.opciones()))
                .toList();
        return new ConversarEnTallerResponse(respuesta.texto(), acciones, preguntas);
    }
}
