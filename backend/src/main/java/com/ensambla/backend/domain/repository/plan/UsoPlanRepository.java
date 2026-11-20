package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface UsoPlanRepository {

    /** Mantiene el bloqueo hasta el fin de la transacción que valida y realiza el alta o la reserva. */
    void bloquearNegocio(NegocioId negocioId);

    long contarProyectos(NegocioId negocioId);

    long contarRegistros(NegocioId negocioId);

    long contarRegistrosProyecto(NegocioId negocioId, ProyectoId proyectoId);

    /** Páginas procesadas y reservadas, desde inclusive y hasta exclusive, en días argentinos. */
    long paginasEnPeriodo(NegocioId negocioId, LocalDate desde, LocalDate hasta);

    /** Objetivos con IA guardados desde inclusive hasta exclusive; borrar un objetivo no los descuenta. */
    long objetivosIaEnPeriodo(NegocioId negocioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** Anota que el objetivo gastó una unidad de la cuota mensual. */
    void registrarObjetivoIa(NegocioId negocioId, ObjetivoId objetivoId, OffsetDateTime ahora);

    boolean registrarPaginaInmediata(NegocioId negocioId, LocalDate fecha, int limiteDiario, OffsetDateTime ahora);
}
