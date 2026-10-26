package com.ensambla.backend.domain.repository.sami;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ConsumoSamiRepository {

    /**
     * Reserva una consulta del día de forma atómica por negocio.
     *
     * @return {@code false} si ya se usaron las {@code limite} consultas del día
     */
    boolean reservar(NegocioId negocioId, LocalDate fecha, int limite, OffsetDateTime ahora);

    /** Suma los tokens de una consulta ya reservada. */
    void registrar(NegocioId negocioId, LocalDate fecha, ConsumoIaSami consumo, OffsetDateTime ahora);
}
