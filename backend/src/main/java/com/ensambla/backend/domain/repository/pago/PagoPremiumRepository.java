package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PagoPremiumRepository {

    PagoPremium save(PagoPremium pago);

    Optional<PagoPremium> findByFacturaId(String facturaId);

    Optional<PagoPremium> findVigente(UsuarioId usuarioId, OffsetDateTime ahora);

    boolean existeByUsuarioId(UsuarioId usuarioId);
}
