package com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami;

import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/**
 * Lo que la persona aceptó de la propuesta de Sami. De un mini-objetivo medido sólo se dice cuál es: el servidor vuelve a
 * calcular sus números.
 *
 * @param seguimientoManual si guarda el seguimiento manual de respaldo: se ignoran {@code minis}
 */
public record GuardarObjetivoConSamiRequest(
        UsuarioId usuarioId, String texto, boolean seguimientoManual, List<MiniElegido> minis) {

    /** Un mini-objetivo elegido: el medido con su {@code tipo}, producto y referencia; el manual con su texto. */
    public record MiniElegido(
            ModoMiniObjetivo modo, TipoMiniObjetivo tipo, RegistroId registroId, String referencia, String titulo,
            String justificacion) {
    }
}
