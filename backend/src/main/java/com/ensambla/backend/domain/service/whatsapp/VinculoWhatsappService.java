package com.ensambla.backend.domain.service.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.GeneradorCodigoPort;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VinculoWhatsappService {

    private final VinculoWhatsappRepository vinculoWhatsappRepository;

    private final GeneradorCodigoPort generadorCodigoPort;

    /** Da un código nuevo al negocio (reemplaza el anterior) para que la persona lo mande desde su WhatsApp. */
    @Transactional
    public VinculoWhatsapp generarCodigo(UUID negocioId, OffsetDateTime ahora) {
        VinculoWhatsapp vinculo = vinculoWhatsappRepository.findByNegocioId(negocioId)
                .orElseGet(() -> VinculoWhatsapp.nuevo(negocioId, ahora));
        vinculo.asignarCodigo(codigoLibre(), ahora);
        return vinculoWhatsappRepository.save(vinculo);
    }

    /**
     * Vincula el número con el negocio dueño del código. Si el número estaba vinculado a otro negocio, deja ese
     * vínculo: quien manda el código tiene el teléfono y la cuenta del negocio nuevo.
     *
     * @return false si el código no existe o venció; en ese caso no cambia nada
     */
    @Transactional
    public boolean vincular(String codigo, String telefono, OffsetDateTime ahora) {
        Optional<VinculoWhatsapp> vinculo = vinculoWhatsappRepository.findByCodigo(codigo)
                .filter(candidato -> candidato.codigoVigente(ahora));
        vinculo.ifPresent(nuevo -> {
            liberarTelefono(telefono, nuevo.getNegocioId(), ahora);
            nuevo.vincular(telefono, ahora);
            vinculoWhatsappRepository.save(nuevo);
        });
        return vinculo.isPresent();
    }

    /** El negocio del número; vacío si el número no está vinculado a ninguno. */
    public Optional<UUID> negocioDelTelefono(String telefono) {
        return vinculoWhatsappRepository.findByTelefono(telefono)
                .map(VinculoWhatsapp::getNegocioId);
    }

    private String codigoLibre() {
        String codigo = generadorCodigoPort.generar();
        while (vinculoWhatsappRepository.existsByCodigo(codigo)) {
            codigo = generadorCodigoPort.generar();
        }
        return codigo;
    }

    private void liberarTelefono(String telefono, UUID negocioId, OffsetDateTime ahora) {
        vinculoWhatsappRepository.findByTelefono(telefono)
                .filter(anterior -> !anterior.getNegocioId().equals(negocioId))
                .ifPresent(anterior -> {
                    anterior.desvincular(ahora);
                    vinculoWhatsappRepository.save(anterior);
                });
    }
}
