package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.caja.CierreCajaId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.Optional;
import java.util.List;
import java.time.OffsetDateTime;

public interface VentaCajaRepository {

    Optional<VentaCaja> findByIdAndNegocioId(VentaCajaId id, NegocioId negocioId);

    VentaCaja save(VentaCaja venta);

    List<VentaCaja> listarAbiertas(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId);

    List<VentaCaja> listarEntre(NegocioId negocioId, ProyectoId proyectoId, BloqueId inventarioId,
            OffsetDateTime desde, OffsetDateTime hasta);

    void marcarCerradas(NegocioId negocioId, CierreCajaId cierreId, List<VentaCajaId> ventas);
}
