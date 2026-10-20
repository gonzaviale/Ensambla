package com.ensambla.backend.domain.useCase.whatsapp;

import com.ensambla.backend.domain.dto.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.enums.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.service.whatsapp.RecepcionWhatsappService;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Atiende un mensaje que llegó por WhatsApp: vincula el número si trae un código, y si no lo guarda o lo rechaza. */
@RequiredArgsConstructor
public class ProcesarMensajeEntranteUseCase {

    static final String RESPUESTA_VINCULADO = "Listo, tu número quedó vinculado a tu negocio en Ensambla.";
    static final String RESPUESTA_NO_VINCULADO = "Este número no está vinculado a ningún negocio de Ensambla. "
            + "Generá un código desde la app y mandame: VINCULAR <código>";

    private static final Pattern COMANDO_VINCULAR = Pattern.compile("^\\s*vincular\\s+(\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final VinculacionWhatsappService vinculacionService;
    private final RecepcionWhatsappService recepcionService;
    private final MensajeriaWhatsappPort mensajeria;
    private final Clock clock;

    public void execute(MensajeEntrante entrante) {
        Optional<String> codigo = codigoDeVinculacion(entrante);
        if (codigo.isPresent()) {
            vincular(entrante, codigo.get());
            return;
        }
        Optional<VinculoWhatsapp> vinculo = vinculacionService.buscarPorTelefono(
                new TelefonoWhatsapp(entrante.telefono()));
        if (vinculo.isEmpty()) {
            mensajeria.enviarTexto(entrante.telefono(), RESPUESTA_NO_VINCULADO);
            return;
        }
        recepcionService.registrar(vinculo.get(), entrante);
    }

    private void vincular(MensajeEntrante entrante, String codigo) {
        String respuesta;
        try {
            vinculacionService.canjear(codigo, new TelefonoWhatsapp(entrante.telefono()), OffsetDateTime.now(clock));
            respuesta = RESPUESTA_VINCULADO;
        } catch (BusinessRuleException e) {
            respuesta = e.getMessage();
        }
        mensajeria.enviarTexto(entrante.telefono(), respuesta);
    }

    private static Optional<String> codigoDeVinculacion(MensajeEntrante entrante) {
        if (entrante.tipo() != TipoMensajeWhatsapp.TEXTO) {
            return Optional.empty();
        }
        Matcher comando = COMANDO_VINCULAR.matcher(entrante.texto());
        return comando.matches() ? Optional.of(comando.group(1)) : Optional.empty();
    }
}
