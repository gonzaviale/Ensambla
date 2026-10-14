package com.ensambla.backend.domain.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EditarCampoMapper {

    private final CampoItemMapper campoItemMapper;

    public ConfiguracionCampo toConfiguracion(EditarCampoRequest request) {
        return new ConfiguracionCampo(
                request.nombre(),
                request.tipo(),
                request.obligatorio(),
                request.valorInicial(),
                request.opciones(),
                request.bloqueRelacionadoId(),
                request.formula());
    }

    public CampoItem toResponse(Campo campo) {
        return campoItemMapper.toResponse(campo);
    }
}
