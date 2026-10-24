package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;

import java.util.List;

/** El sistema que se está armando en el taller, resumido: nombre, distribución y piezas con sus campos. */
public record EstadoTallerItem(String nombre, String distribucion, List<PiezaTallerItem> piezas) {

    public record PiezaTallerItem(String id, String tipo, String nombre, String seccion, List<CampoTallerItem> campos) {
    }

    public record CampoTallerItem(String etiqueta, String tipo) {
    }

    EstadoTaller aEstadoTaller() {
        List<PiezaTaller> piezasTaller = piezas.stream()
                .map(pieza -> new PiezaTaller(
                        pieza.id(),
                        pieza.tipo(),
                        pieza.nombre(),
                        pieza.seccion(),
                        pieza.campos().stream()
                                .map(campo -> new CampoTaller(campo.etiqueta(), campo.tipo()))
                                .toList()))
                .toList();
        return new EstadoTaller(nombre, distribucion, piezasTaller);
    }
}
