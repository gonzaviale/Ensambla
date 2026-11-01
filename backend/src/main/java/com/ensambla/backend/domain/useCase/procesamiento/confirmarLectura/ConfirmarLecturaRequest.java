package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;
import java.util.Map;

public record ConfirmarLecturaRequest(
        UsuarioId usuarioId,
        LoteId loteId,
        ProyectoId proyectoId,
        String nombreProyecto,
        BloqueId bloqueId,
        String nombreBloque,
        List<CampoItem> campos,
        List<FilaItem> filas
) {

    public record CampoItem(String clave, CampoId campoId, String nombre, String tipo, List<String> opciones) {
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
