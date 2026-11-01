package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;

import java.util.List;
import java.util.Optional;

public interface RenovacionPremiumRepository {

    RenovacionPremium save(RenovacionPremium renovacion);

    Optional<RenovacionPremium> findById(RenovacionId id);

    Optional<NegocioId> findNegocioIdById(RenovacionId id);

    Optional<RenovacionPremium> findByProveedorId(String proveedorId);

    Optional<RenovacionPremium> findUltimaByNegocioId(NegocioId negocioId);

    boolean existeAbierta(NegocioId negocioId);

    List<RenovacionId> listarParaSincronizar();
}
