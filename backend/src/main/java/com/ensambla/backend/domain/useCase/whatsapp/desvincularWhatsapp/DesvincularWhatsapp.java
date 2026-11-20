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
        desvincularEnTransaccion(request.usuarioId());
    }

    private void desvincularEnTransaccion(UsuarioId usuarioId) {
        transaccionPort.ejecutar(() -> desvincular(usuarioId));
    }

    private void desvincular(UsuarioId usuarioId) {
        vinculoRepository.obtenerDelUsuario(usuarioId);
        vinculoRepository.deleteByUsuarioId(usuarioId);
    }
}
