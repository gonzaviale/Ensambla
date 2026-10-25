package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.PagoPremium;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PagoPremiumRepository {

    PagoPremium save(PagoPremium pago);

    Optional<PagoPremium> findByFacturaId(String facturaId);

    Optional<PagoPremium> findVigente(NegocioId negocioId, OffsetDateTime ahora);

    boolean existeByNegocioId(NegocioId negocioId);
}
