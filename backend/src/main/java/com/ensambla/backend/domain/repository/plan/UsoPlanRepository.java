package com.ensambla.backend.domain.repository.plan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface UsoPlanRepository {

    /** Mantiene el bloqueo hasta el fin de la transacción que valida y realiza el alta o la reserva. */
    void bloquearNegocio(UUID negocioId);

    long contarProyectos(UUID negocioId);

    long contarRegistros(UUID negocioId);

    long contarRegistrosProyecto(UUID negocioId, UUID proyectoId);

    /** Páginas procesadas y reservadas, desde inclusive y hasta exclusive, en días UTC. */
    long paginasEnPeriodo(UUID negocioId, LocalDate desde, LocalDate hasta);

    boolean registrarPaginaInmediata(UUID negocioId, LocalDate fecha, int limiteDiario, OffsetDateTime ahora);
}
