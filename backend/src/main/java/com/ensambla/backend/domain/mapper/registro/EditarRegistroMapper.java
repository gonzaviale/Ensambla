package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.model.registro.Registro;

public class EditarRegistroMapper {

    public EditarRegistroResponse toResponse(Registro registro) {
        return new EditarRegistroResponse(RegistroItem.de(registro));
    }
}
