package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface UsoPlanRepository {

    /**
     * Bloquea la cuenta dueña del negocio y mantiene el bloqueo hasta el fin de la transacción que valida y realiza el
     * alta o la reserva. El plan, la prueba y el cupo mensual de IA son de la cuenta: bloquear la cuenta serializa
     * todos los negocios de una misma cuenta con sus cambios de plan.
     */
    void bloquearNegocio(NegocioId negocioId);

    /** Lo mismo que {@link #bloquearNegocio} para lo que se hace sobre la cuenta (prueba, contratación, vencimiento). */
    void bloquearCuenta(UsuarioId usuarioId);

    /** Cuántos negocios tiene la cuenta: es lo que limita el plan. */
    long contarNegocios(UsuarioId usuarioId);

    long contarRegistros(NegocioId negocioId);

    long contarRegistrosProyecto(NegocioId negocioId, ProyectoId proyectoId);

    /**
     * Páginas procesadas y reservadas por todos los negocios de la cuenta, desde inclusive y hasta exclusive, en días
     * argentinos: el cupo mensual de IA es del plan, que es de la cuenta.
     */
    long paginasEnPeriodo(UsuarioId usuarioId, LocalDate desde, LocalDate hasta);

    boolean registrarPaginaInmediata(NegocioId negocioId, LocalDate fecha, int limiteDiario, OffsetDateTime ahora);
}
