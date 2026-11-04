package com.ensambla.backend.domain.useCase.zona.obtenerZona;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.VistaZona;

/** {@code vista}: la pantalla de Mi zona que pide los datos; define el orden y el cupo gratuito que se gasta. */
public record ObtenerZonaRequest(UsuarioId usuarioId, VistaZona vista) {
}
