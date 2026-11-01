package com.ensambla.backend.domain.useCase.registro.crearRegistro;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;

public record CrearRegistroResponse(RegistroItem registro) {

    public static CrearRegistroResponse de(Registro registro) {
        return new CrearRegistroResponse(RegistroItem.de(registro));
    }
}
