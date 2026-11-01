package com.ensambla.backend.domain.useCase.registro.editarRegistro;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.useCase.registro.RegistroItem;

public record EditarRegistroResponse(RegistroItem registro) {

    public static EditarRegistroResponse de(Registro registro) {
        return new EditarRegistroResponse(RegistroItem.de(registro));
    }
}
