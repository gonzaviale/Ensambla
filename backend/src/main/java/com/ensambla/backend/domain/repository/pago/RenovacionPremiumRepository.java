package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.pago.RenovacionPremium;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenovacionPremiumRepository {

    RenovacionPremium save(RenovacionPremium renovacion);

    Optional<RenovacionPremium> findById(UUID id);

    Optional<UUID> findNegocioIdById(UUID id);

    Optional<RenovacionPremium> findByProveedorId(String proveedorId);

    Optional<RenovacionPremium> findUltimaByNegocioId(UUID negocioId);

    boolean existeAbierta(UUID negocioId);

    List<UUID> listarParaSincronizar();
}
