package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.pago.PagoPremium;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PagoPremiumRepository {

    PagoPremium save(PagoPremium pago);

    Optional<PagoPremium> findByFacturaId(String facturaId);

    Optional<PagoPremium> findVigente(UUID negocioId, OffsetDateTime ahora);

    boolean existeByNegocioId(UUID negocioId);
}
