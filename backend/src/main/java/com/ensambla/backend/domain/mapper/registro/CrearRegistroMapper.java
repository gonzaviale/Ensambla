package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.model.registro.Registro;

public class CrearRegistroMapper {

    public CrearRegistroResponse toResponse(Registro registro) {
        return new CrearRegistroResponse(RegistroItem.de(registro));
    }
}
