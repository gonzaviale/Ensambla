package com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConsultarVinculacionWhatsapp {

    private final VinculoWhatsappRepository vinculoRepository;
    private final MensajeriaWhatsappPort mensajeria;

    public ConsultarVinculacionWhatsappResponse execute(ConsultarVinculacionWhatsappRequest request) {
        return ConsultarVinculacionWhatsappResponse.de(obtenerVinculoDelUsuario(request.usuarioId()), numeroDelBot());
    }

    private VinculoWhatsapp obtenerVinculoDelUsuario(UsuarioId usuarioId) {
        return vinculoRepository.obtenerDelUsuario(usuarioId);
    }

    private String numeroDelBot() {
        return mensajeria.numeroBot();
    }
}
