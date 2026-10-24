package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository {

    Optional<NegocioId> findIdByDuenioId(UsuarioId usuarioId);

    Optional<Negocio> findByDuenioId(UsuarioId usuarioId);

    Optional<Negocio> findByDuenioIdParaActualizar(UsuarioId usuarioId);

    Negocio save(Negocio negocio);

    /** Ids de todos los negocios; los usa el motor de monitoreo para recorrerlos. */
    List<NegocioId> findAllIds();
}

