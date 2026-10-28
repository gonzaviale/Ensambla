package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * El sistema que se está armando en el taller, resumido: nombre, distribución y piezas con sus campos y sus registros
 * reales.
 */
public record EstadoTallerItem(String nombre, String distribucion, List<PiezaTallerItem> piezas) {

    public record PiezaTallerItem(String id, String tipo, String nombre, String seccion, List<CampoTallerItem> campos,
                                  List<RegistroTallerItem> registros, List<String> nombres) {

        public PiezaTallerItem(String id, String tipo, String nombre, String seccion, List<CampoTallerItem> campos,
                               List<RegistroTallerItem> registros) {
            this(id, tipo, nombre, seccion, campos, registros, List.of());
        }

        public PiezaTallerItem(String id, String tipo, String nombre, String seccion, List<CampoTallerItem> campos) {
            this(id, tipo, nombre, seccion, campos, List.of());
        }
    }

    public record CampoTallerItem(String etiqueta, String tipo) {
    }

    public record RegistroTallerItem(String id, Map<String, Object> datos) {
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
                                .toList(),
                        Objects.requireNonNullElse(pieza.registros(), List.<RegistroTallerItem>of()).stream()
                                .map(registro -> new RegistroTaller(registro.id(),
                                        Objects.requireNonNullElse(registro.datos(), Map.of())))
                                .toList(),
                        pieza.nombres()))
                .toList();
        return new EstadoTaller(nombre, distribucion, piezasTaller);
    }
}
