package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.MensajeEntranteWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MensajeWhatsappService {

    private final MensajeWhatsappRepository mensajeWhatsappRepository;

    private final VinculoWhatsappService vinculoWhatsappService;

    /**
     * Un código de vinculación vigente vincula el número y no se guarda como mensaje. Cualquier otro mensaje se
     * guarda en el negocio del número; si el número no está vinculado se descarta, así no carga datos en ningún
     * negocio.
     */
    @Transactional
    public void recibir(MensajeEntranteWhatsapp entrante, OffsetDateTime ahora) {
        if (vinculaElNumero(entrante, ahora)) {
            return;
        }
        vinculoWhatsappService.negocioDelTelefono(entrante.telefono())
                .ifPresent(negocioId -> guardar(negocioId, entrante, ahora));
    }

    private boolean vinculaElNumero(MensajeEntranteWhatsapp entrante, OffsetDateTime ahora) {
        return entrante.codigoDeVinculacion()
                .map(codigo -> vinculoWhatsappService.vincular(codigo, entrante.telefono(), ahora))
                .orElse(false);
    }

    /** Meta reintenta el aviso si no le respondemos a tiempo: un mensaje repetido no se guarda dos veces. */
    private void guardar(UUID negocioId, MensajeEntranteWhatsapp entrante, OffsetDateTime ahora) {
        if (!mensajeWhatsappRepository.existsByWaMensajeId(entrante.waMensajeId())) {
            mensajeWhatsappRepository.save(MensajeWhatsapp.nuevo(negocioId, entrante, ahora));
        }
    }
}
