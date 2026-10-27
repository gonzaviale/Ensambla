package com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;
import com.ensambla.backend.domain.model.whatsapp.MensajeEntrante;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.CodigoVinculacionRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.whatsapp.VinculacionWhatsappService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atiende al instante lo que llega por WhatsApp: vincula el número si trae un código, rechaza a quien no está
 * vinculado y deja el resto en la cola para que Sami lo atienda sin hacer esperar al proveedor.
 */
@RequiredArgsConstructor
public class ProcesarMensajeEntrante {

    private static final Pattern COMANDO_VINCULAR = Pattern.compile("^\\s*vincular\\s+(\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final NegocioRepository negocioRepository;
    private final CodigoVinculacionRepository codigoRepository;
    private final VinculoWhatsappRepository vinculoRepository;
    private final MensajeWhatsappRepository mensajeRepository;
    private final VinculacionWhatsappService vinculacionService;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public void execute(MensajeEntrante entrante) {
        Optional<String> codigo = codigoDeVinculacion(entrante);
        if (codigo.isPresent()) {
            vincular(entrante, codigo.get());
            return;
        }
        Optional<VinculoWhatsapp> vinculo = vinculoRepository.findByTelefono(entrante.telefono());
        if (vinculo.isEmpty()) {
            mensajeria.enviarTexto(entrante.telefono(), TextosWhatsapp.NO_VINCULADO);
            return;
        }
        transaccionPort.ejecutar(() -> encolar(vinculo.get(), entrante));
    }

    private void vincular(MensajeEntrante entrante, String textoCodigo) {
        String respuesta;
        try {
            TelefonoWhatsapp telefono = new TelefonoWhatsapp(entrante.telefono());
            transaccionPort.ejecutar(() -> canjear(textoCodigo, telefono, OffsetDateTime.now(clock)));
            respuesta = TextosWhatsapp.VINCULADO;
        } catch (BusinessRuleException | DuplicateResourceException e) {
            respuesta = e.getMessage();
        }
        mensajeria.enviarTexto(entrante.telefono(), respuesta);
    }

    /** Vincula el teléfono con el negocio del usuario que generó el código; si ya tenía un número, lo reemplaza. */
    private void canjear(String textoCodigo, TelefonoWhatsapp telefono, OffsetDateTime ahora) {
        CodigoVinculacion codigo = codigoRepository.findByCodigo(textoCodigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessRuleException("El código de vinculación no existe"));
        codigo.usar(ahora);
        UsuarioId usuarioId = codigo.getUsuarioId();
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(usuarioId);
        vinculacionService.vincular(usuarioId, negocioId, telefono, ahora);
        codigoRepository.save(codigo);
    }

    /** Guarda el mensaje en el negocio del vínculo; si ya estaba (el proveedor reintentó el webhook) no hace nada. */
    private void encolar(VinculoWhatsapp vinculo, MensajeEntrante entrante) {
        if (mensajeRepository.existsByIdExterno(entrante.idExterno())) {
            return;
        }
        mensajeRepository.save(MensajeWhatsapp.recibido(vinculo.getNegocioId(), entrante));
    }

    private static Optional<String> codigoDeVinculacion(MensajeEntrante entrante) {
        if (entrante.tipo() != TipoMensajeWhatsapp.TEXTO) {
            return Optional.empty();
        }
        Matcher comando = COMANDO_VINCULAR.matcher(entrante.texto());
        return comando.matches() ? Optional.of(comando.group(1)) : Optional.empty();
    }
}
