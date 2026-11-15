package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.CobroCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CobroCajaRepository {

    Optional<CobroCaja> findByIdAndNegocioId(CobroCajaId id, NegocioId negocioId);

    CobroCaja save(CobroCaja cobro);

    List<CobroCaja> listarPorProyecto(NegocioId negocioId, ProyectoId proyectoId);

    List<CobroCaja> listarTodos(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId);

    List<CobroCaja> listarAbiertos(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId);

    List<CobroCaja> listarEntre(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
            OffsetDateTime desde, OffsetDateTime hasta);

    void marcarCerrados(NegocioId negocioId, CierreCajaId cierreId, List<CobroCajaId> cobros);
}
