package com.ensambla.backend.domain.repository.pago;

import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface RenovacionPremiumRepository {

    RenovacionPremium save(RenovacionPremium renovacion);

    Optional<RenovacionPremium> findById(RenovacionId id);

    Optional<UsuarioId> findUsuarioIdById(RenovacionId id);

    Optional<RenovacionPremium> findByProveedorId(String proveedorId);

    Optional<RenovacionPremium> findUltimaByUsuarioId(UsuarioId usuarioId);

    boolean existeAbierta(UsuarioId usuarioId);

    List<RenovacionId> listarParaSincronizar();
}
