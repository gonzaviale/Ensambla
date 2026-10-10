package com.ensambla.backend.domain.mapper.registro;

import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.model.registro.Registro;

public class ObtenerRegistroMapper {

    public ObtenerRegistroResponse toResponse(Registro registro) {
        return new ObtenerRegistroResponse(RegistroItem.de(registro));
    }
}
