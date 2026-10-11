package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaRequest;
import com.ensambla.backend.domain.dto.procesamiento.ConfirmarLecturaResponse;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.CampoConfirmado;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionLectura.FilaConfirmada;

public class ConfirmarLecturaMapper {

    public ConfirmacionLectura toConfirmacion(ConfirmarLecturaRequest request) {
        return new ConfirmacionLectura(
                request.proyectoId(),
                request.nombreProyecto(),
                request.bloqueId(),
                request.nombreBloque(),
                request.campos().stream()
                        .map(campo -> new CampoConfirmado(
                                campo.clave(), campo.campoId(), campo.nombre(), campo.tipo(), campo.opciones()))
                        .toList(),
                request.filas().stream()
                        .map(fila -> new FilaConfirmada(fila.pagina(), fila.valores()))
                        .toList());
    }

    public ConfirmarLecturaResponse toResponse(ConfirmacionGuardada guardada) {
        return new ConfirmarLecturaResponse(guardada.proyectoId(), guardada.bloqueId(), guardada.registros());
    }
}
