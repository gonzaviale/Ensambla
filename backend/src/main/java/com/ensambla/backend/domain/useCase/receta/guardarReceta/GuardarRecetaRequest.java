package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/** Reemplaza la receta del producto (un registro); sin líneas, la quita. */
public record GuardarRecetaRequest(
        UsuarioId usuarioId,
        RegistroId registroId,
        CampoId campoPrecioId,
        List<LineaRecetaRequest> lineas) {

    public List<LineaReceta> aLineasReceta() {
        return lineas.stream()
                .map(linea -> new LineaReceta(linea.insumoId(), linea.cantidad(), linea.unidad()))
                .toList();
    }
}
