package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface UsoPlanRepository {

    /** Mantiene el bloqueo hasta el fin de la transacción que valida y realiza el alta o la reserva. */
    void bloquearNegocio(NegocioId negocioId);

    long contarProyectos(NegocioId negocioId);

    long contarRegistros(NegocioId negocioId);

    long contarRegistrosProyecto(NegocioId negocioId, ProyectoId proyectoId);

    /** Páginas procesadas y reservadas, desde inclusive y hasta exclusive, en días UTC. */
    long paginasEnPeriodo(NegocioId negocioId, LocalDate desde, LocalDate hasta);

    boolean registrarPaginaInmediata(NegocioId negocioId, LocalDate fecha, int limiteDiario, OffsetDateTime ahora);
}
