package com.ensambla.backend.domain.repository.sami;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ConsumoSamiRepository {

    /**
     * Reserva una consulta del día de forma atómica por negocio.
     *
     * @return {@code false} si ya se usaron las {@code limite} consultas del día
     */
    boolean reservar(UUID negocioId, LocalDate fecha, int limite, OffsetDateTime ahora);

    /** Suma los tokens de una consulta ya reservada. */
    void registrar(UUID negocioId, LocalDate fecha, ConsumoIaSami consumo, OffsetDateTime ahora);
}
