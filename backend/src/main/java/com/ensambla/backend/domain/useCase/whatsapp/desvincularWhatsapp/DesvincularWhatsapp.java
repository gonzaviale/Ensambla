package com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DesvincularWhatsapp {

    private final VinculoWhatsappRepository vinculoRepository;
    private final TransaccionPort transaccionPort;

    public void execute(DesvincularWhatsappRequest request) {
        transaccionPort.ejecutar(() -> desvincular(request.usuarioId()));
    }

    private void desvincular(UsuarioId usuarioId) {
        vinculoRepository.obtenerDelUsuario(usuarioId);
        vinculoRepository.deleteByUsuarioId(usuarioId);
    }
}
