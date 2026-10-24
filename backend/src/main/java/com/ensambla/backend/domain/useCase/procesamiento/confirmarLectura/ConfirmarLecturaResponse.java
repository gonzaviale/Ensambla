package com.ensambla.backend.domain.useCase.procesamiento.confirmarLectura;

import com.ensambla.backend.domain.model.procesamiento.ConfirmacionGuardada;

import java.util.UUID;

public record ConfirmarLecturaResponse(UUID proyectoId, UUID bloqueId, int registros) {

    public static ConfirmarLecturaResponse de(ConfirmacionGuardada guardada) {
        return new ConfirmarLecturaResponse(guardada.proyectoId(), guardada.bloqueId(), guardada.registros());
    }
}
