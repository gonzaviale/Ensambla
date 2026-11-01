package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;

import java.util.List;
import java.util.Optional;

/**
 * Estructura que la IA propone para guardar lo leído.
 *
 * @param bloqueExistenteId bloque del negocio al que conviene sumar las filas, o {@code null} si propone uno nuevo
 */
public record BloquePropuesto(String nombre, BloqueId bloqueExistenteId, List<CampoPropuesto> campos) {

    public BloquePropuesto {
        campos = List.copyOf(campos);
    }

    public Optional<BloqueId> bloqueExistente() {
        return Optional.ofNullable(bloqueExistenteId);
    }
}
