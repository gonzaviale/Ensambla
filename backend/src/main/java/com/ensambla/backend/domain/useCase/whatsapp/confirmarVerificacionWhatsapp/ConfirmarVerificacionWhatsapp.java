package com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Vincula el número cuando la persona escribe en la app el código que le llegó por WhatsApp. */
@RequiredArgsConstructor
public class ConfirmarVerificacionWhatsapp {

    private final NegocioRepository negocioRepository;
    private final VerificacionWhatsappRepository verificacionRepository;
    private final VinculacionWhatsappService vinculacionService;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public ConfirmarVerificacionWhatsappResponse execute(ConfirmarVerificacionWhatsappRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        VerificacionWhatsapp verificacion = verificacionRepository.obtenerDelUsuario(request.usuarioId());
        confirmar(verificacion, request.codigo(), ahora);
        VinculoWhatsapp vinculo = transaccionPort.ejecutar(() -> vinculacionService.vincular(
                request.usuarioId(), negocioId, new TelefonoWhatsapp(verificacion.getTelefono()), ahora));
        mensajeria.enviarTexto(vinculo.getTelefono(), TextosWhatsapp.BIENVENIDA);
        return ConfirmarVerificacionWhatsappResponse.de(vinculo, mensajeria.numeroBot());
    }

    /** El intento se cuenta aunque el código sea incorrecto: por eso se guarda antes de avisar del error. */
    private void confirmar(VerificacionWhatsapp verificacion, String codigo, OffsetDateTime ahora) {
        try {
            verificacion.confirmar(codigo, ahora);
        } catch (BusinessRuleException e) {
            transaccionPort.ejecutar(() -> verificacionRepository.save(verificacion));
            throw e;
        }
    }
}
