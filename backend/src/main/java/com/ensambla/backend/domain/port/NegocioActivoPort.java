package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.Optional;

/** Guarda en la sesión cuál de los negocios de la cuenta se está usando. */
public interface NegocioActivoPort {

    Optional<NegocioId> obtener(UsuarioId usuarioId);

    void activar(UsuarioId usuarioId, NegocioId negocioId);
}
