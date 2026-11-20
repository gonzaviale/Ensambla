package com.ensambla.backend.infrastructure.presentation.mapper.caja;

import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.domain.useCase.caja.cancelarCobroQr.CancelarCobroQrRequest;
import com.ensambla.backend.domain.useCase.caja.generarCobroQr.GenerarCobroQrRequest;
import com.ensambla.backend.infrastructure.presentation.dto.caja.CobroQrWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.caja.GenerarCobroQrWebRequest;
import org.springframework.stereotype.Component;

@Component
public class CobroQrPresentationMapper {

    public GenerarCobroQrRequest toGenerarRequest(UsuarioId usuarioId, GenerarCobroQrWebRequest request) {
        return new GenerarCobroQrRequest(usuarioId, request.id(), request.importe());
    }

    public CancelarCobroQrRequest toCancelarRequest(UsuarioId usuarioId, CobroQrId id) {
        return new CancelarCobroQrRequest(usuarioId, id);
    }

    public CobroQrWebResponse toResponse(CobroQrItem cobro) {
        return new CobroQrWebResponse(cobro.id(), cobro.importe(), cobro.alias(), cobro.titular(),
                cobro.estado(), cobro.venceEn());
    }
}
