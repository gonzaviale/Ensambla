package com.ensambla.backend.domain.useCase.registro.obtenerRegistro;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;

public record ObtenerRegistroResponse(RegistroItem registro) {

    public static ObtenerRegistroResponse de(Registro registro) {
        return new ObtenerRegistroResponse(RegistroItem.de(registro));
    }
}
