package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;

import java.time.OffsetDateTime;

/**
 * {@code id} es el de la URL y manda sobre el que traiga el cuerpo.
 *
 * @param leidoEn cuándo leyó el cliente los sistemas de los que parte este reemplazo; las filas que Sami cargó por WhatsApp después
 *                no las conocía y no se eliminan. Null conserva el reemplazo exacto.
 */
public record GuardarSistemaRequest(UsuarioId usuarioId, String id, SistemaItem sistema, OffsetDateTime leidoEn) {

    public GuardarSistemaRequest(UsuarioId usuarioId, String id, SistemaItem sistema) {
        this(usuarioId, id, sistema, null);
    }
}
