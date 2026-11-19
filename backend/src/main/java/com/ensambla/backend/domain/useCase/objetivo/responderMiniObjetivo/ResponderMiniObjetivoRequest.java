package com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ResponderMiniObjetivoRequest(
        UsuarioId usuarioId,
        ObjetivoId objetivoId,
        MiniObjetivoId miniObjetivoId,
        HizoManual hizo,
        QuePaso quePaso,
        String comentario) {
}
