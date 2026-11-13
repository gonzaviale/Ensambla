package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PruebaPremiumRepository {

    Optional<PruebaPremium> findByUsuarioId(UsuarioId usuarioId);

    PruebaPremium save(PruebaPremium prueba);

    List<UsuarioId> findUsuariosVencidos(OffsetDateTime ahora);
}
