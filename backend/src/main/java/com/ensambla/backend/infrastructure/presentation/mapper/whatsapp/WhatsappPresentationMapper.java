package com.ensambla.backend.infrastructure.presentation.mapper.whatsapp;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsappResponse;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsappRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionRequest;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoVinculacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.VinculoWhatsappResponse;
import org.springframework.stereotype.Component;

@Component
public class WhatsappPresentationMapper {

    public GenerarCodigoVinculacionRequest toGenerarCodigoRequest(UsuarioId usuarioId) {
        return new GenerarCodigoVinculacionRequest(usuarioId);
    }

    public ConsultarVinculacionWhatsappRequest toConsultarRequest(UsuarioId usuarioId) {
        return new ConsultarVinculacionWhatsappRequest(usuarioId);
    }

    public DesvincularWhatsappRequest toDesvincularRequest(UsuarioId usuarioId) {
        return new DesvincularWhatsappRequest(usuarioId);
    }

    public CodigoVinculacionResponse toResponse(GenerarCodigoVinculacionResponse response) {
        return new CodigoVinculacionResponse(response.codigo(), response.expiraEn(), response.numeroBot());
    }

    public VinculoWhatsappResponse toResponse(ConsultarVinculacionWhatsappResponse response) {
        return new VinculoWhatsappResponse(response.telefono(), response.vinculadoEn());
    }
}
