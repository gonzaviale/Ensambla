package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfirmarLecturaRequest(
        UUID usuarioId,
        UUID loteId,
        UUID proyectoId,
        String nombreProyecto,
        UUID bloqueId,
        String nombreBloque,
        List<CampoItem> campos,
        List<FilaItem> filas
) {

    public record CampoItem(String clave, UUID campoId, String nombre, String tipo, List<String> opciones) {
    }

    public record FilaItem(Integer pagina, Map<String, Object> valores) {
    }

    public ConfirmacionLectura aConfirmacionLectura() {
        return new ConfirmacionLectura(
                proyectoId,
                nombreProyecto,
                bloqueId,
                nombreBloque,
                campos.stream()
                        .map(campo -> new CampoConfirmado(
                                campo.clave(), campo.campoId(), campo.nombre(), campo.tipo(), campo.opciones()))
                        .toList(),
                filas.stream()
                        .map(fila -> new FilaConfirmada(fila.pagina(), fila.valores()))
                        .toList());
    }
}
