package com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/**
 * Cambia un mini-objetivo en curso por un candidato. {@code tipo}, {@code registroId} y {@code referencia} son los
 * del candidato que se eligió de la lista de alternativas.
 */
public record ReemplazarMiniObjetivoRequest(
        UsuarioId usuarioId,
        ObjetivoId objetivoId,
        MiniObjetivoId miniObjetivoId,
        TipoMiniObjetivo tipo,
        RegistroId registroId,
        String referencia) {
}
