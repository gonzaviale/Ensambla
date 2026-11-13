package com.ensambla.backend.domain.repository.caja;

import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.caja.VentaCajaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.Optional;

public interface VentaCajaRepository {

    Optional<VentaCaja> findByIdAndNegocioId(VentaCajaId id, NegocioId negocioId);

    VentaCaja save(VentaCaja venta);
}
