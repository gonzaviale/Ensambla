package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Vincula un número de WhatsApp con un usuario una vez que demostró que es suyo, ya sea mandando el código desde el
 * teléfono o escribiendo en la app el que le llegó por WhatsApp. Lo comparten los dos caminos de vinculación.
 */
@RequiredArgsConstructor
public class VinculacionWhatsappService {

    private final VinculoWhatsappRepository vinculoRepository;
    private final VerificacionWhatsappRepository verificacionRepository;

    /** Quien llama abre la transacción. Si el usuario ya tenía un número, lo reemplaza. */
    public VinculoWhatsapp vincular(UsuarioId usuarioId, NegocioId negocioId, TelefonoWhatsapp telefono,
                                    OffsetDateTime ahora) {
        vinculoRepository.exigirLibre(telefono, usuarioId);
        vinculoRepository.deleteByUsuarioId(usuarioId);
        verificacionRepository.deleteByUsuarioId(usuarioId);
        return vinculoRepository.save(VinculoWhatsapp.nuevo(usuarioId, negocioId, telefono, ahora));
    }
}
