package com.ensambla.backend.domain.model.lectura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Estructura que la IA propone para guardar lo leído.
 *
 * @param bloqueExistenteId bloque del negocio al que conviene sumar las filas, o {@code null} si propone uno nuevo
 */
public record BloquePropuesto(String nombre, UUID bloqueExistenteId, List<CampoPropuesto> campos) {

    public BloquePropuesto {
        campos = List.copyOf(campos);
    }

    public Optional<UUID> bloqueExistente() {
        return Optional.ofNullable(bloqueExistenteId);
    }
}
