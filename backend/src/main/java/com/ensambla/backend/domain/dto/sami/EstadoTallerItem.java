package com.ensambla.backend.domain.dto.sami;

import java.util.List;

/** El sistema que se está armando en el taller, resumido: nombre, distribución y piezas con sus campos y primeras filas. */
public record EstadoTallerItem(String nombre, String distribucion, List<PiezaTallerItem> piezas) {

    public record PiezaTallerItem(String id, String tipo, String nombre, String seccion, List<CampoTallerItem> campos,
                                  List<FilaTallerItem> filas) {
    }

    public record FilaTallerItem(String id, List<String> valores) {
    }

    public record CampoTallerItem(String etiqueta, String tipo) {
    }
}
