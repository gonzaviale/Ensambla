package com.ensambla.backend.domain.useCase.preferencia.configurarAvisosPorWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.preferencia.PreferenciaUsuario;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.preferencia.PreferenciaRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.useCase.preferencia.PreferenciasItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/** La persona pide (o deja de pedir) que Sami le mande por WhatsApp el resumen de la mañana. */
@RequiredArgsConstructor
public class ConfigurarAvisosPorWhatsapp {

    private static final String MENSAJE_SIN_NUMERO = "Primero vinculá tu número de WhatsApp para recibir los avisos";

    private final PreferenciaRepository preferenciaRepository;

    private final VinculoWhatsappRepository vinculoWhatsappRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public PreferenciasItem execute(ConfigurarAvisosPorWhatsappRequest request) {
        exigirNumeroVinculadoSiSePrende(request);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return PreferenciasItem.de(transaccionPort.ejecutar(() -> configurar(request, ahora)));
    }

    private void exigirNumeroVinculadoSiSePrende(ConfigurarAvisosPorWhatsappRequest request) {
        if (request.activo() && vinculoWhatsappRepository.findByUsuarioId(request.usuarioId()).isEmpty()) {
            throw new BusinessRuleException(MENSAJE_SIN_NUMERO);
        }
    }

    private PreferenciaUsuario configurar(ConfigurarAvisosPorWhatsappRequest request, OffsetDateTime ahora) {
        PreferenciaUsuario preferencia = preferenciaRepository.obtenerOInicial(request.usuarioId(), ahora);
        preferencia.configurarAvisosPorWhatsapp(request.activo(), ahora);
        return preferenciaRepository.save(preferencia);
    }
}
