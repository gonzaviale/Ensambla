package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.ZonaId;

import java.util.List;
import java.util.Optional;

public interface NegocioRepository {

    Optional<NegocioId> findIdByDuenioId(UsuarioId usuarioId);

    Optional<Negocio> findByDuenioId(UsuarioId usuarioId);

    Optional<Negocio> findByDuenioIdParaActualizar(UsuarioId usuarioId);

    /** Zona a la que pertenece el negocio; vacío si el negocio no existe o todavía no tiene zona. */
    Optional<ZonaId> findZonaIdById(NegocioId negocioId);

    Negocio save(Negocio negocio);

    /** Ids de todos los negocios; los usa el motor de monitoreo para recorrerlos. */
    List<NegocioId> findAllIds();

    /** El negocio de la sesión; un usuario sin negocio responde como inexistente. */
    default NegocioId obtenerIdDelUsuario(UsuarioId usuarioId) {
        return findIdByDuenioId(usuarioId).orElseThrow(NegocioRepository::negocioInexistente);
    }

    default Negocio obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return findByDuenioId(usuarioId).orElseThrow(NegocioRepository::negocioInexistente);
    }

    private static ResourceNotFoundException negocioInexistente() {
        return new ResourceNotFoundException("No se encontró el negocio del usuario");
    }
}

