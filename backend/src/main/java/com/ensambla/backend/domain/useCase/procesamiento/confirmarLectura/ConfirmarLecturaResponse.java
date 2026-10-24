package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

public record ConfirmarLecturaResponse(ProyectoId proyectoId, BloqueId bloqueId, int registros) {

    public static ConfirmarLecturaResponse de(ConfirmacionGuardada guardada) {
        return new ConfirmarLecturaResponse(guardada.proyectoId(), guardada.bloqueId(), guardada.registros());
    }
}
