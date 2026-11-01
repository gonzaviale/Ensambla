package com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.whatsapp.VerificacionWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/** Manda por WhatsApp un código al número que la persona cargó en la app, para que demuestre que es suyo. */
@RequiredArgsConstructor
public class SolicitarVerificacionWhatsapp {

    /** Cuántas cuentas pueden pedirle un código al mismo número en una hora: evita usarnos para molestar a alguien. */
    static final int MAX_CODIGOS_POR_TELEFONO = 3;

    private static final Duration VENTANA_POR_TELEFONO = Duration.ofHours(1);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final NegocioRepository negocioRepository;
    private final VinculoWhatsappRepository vinculoRepository;
    private final VerificacionWhatsappRepository verificacionRepository;
    private final MensajeriaWhatsappPort mensajeria;
    private final TransaccionPort transaccionPort;
    private final Clock clock;

    public SolicitarVerificacionWhatsappResponse execute(SolicitarVerificacionWhatsappRequest request) {
        negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        TelefonoWhatsapp telefono = TelefonoWhatsapp.deIngresoManual(request.telefono());
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        exigirQueNoSeaElSuyo(request.usuarioId(), telefono);
        vinculoRepository.exigirLibre(telefono, request.usuarioId());
        exigirCupoDelTelefono(telefono, ahora);
        String codigo = nuevoCodigo();
        VerificacionWhatsapp verificacion = transaccionPort.ejecutar(
                () -> guardar(request.usuarioId(), telefono, codigo, ahora));
        enviar(verificacion, telefono, codigo);
        return SolicitarVerificacionWhatsappResponse.de(verificacion);
    }

    private void exigirQueNoSeaElSuyo(UsuarioId usuarioId, TelefonoWhatsapp telefono) {
        boolean esElSuyo = vinculoRepository.findByUsuarioId(usuarioId)
                .filter(vinculo -> vinculo.getTelefono().equals(telefono.valor()))
                .isPresent();
        if (esElSuyo) {
            throw new BusinessRuleException("Ese número ya está vinculado a tu cuenta");
        }
    }

    private void exigirCupoDelTelefono(TelefonoWhatsapp telefono, OffsetDateTime ahora) {
        long enviados = verificacionRepository.contarEnviosRecientesA(
                telefono.valor(), ahora.minus(VENTANA_POR_TELEFONO));
        if (enviados >= MAX_CODIGOS_POR_TELEFONO) {
            throw new BusinessRuleException("Ya mandamos varios códigos a ese número. Probá en una hora");
        }
    }

    /** Si ya había una verificación en curso se reemplaza (respetando la espera y el tope de envíos). */
    private VerificacionWhatsapp guardar(UsuarioId usuarioId, TelefonoWhatsapp telefono, String codigo,
                                         OffsetDateTime ahora) {
        VerificacionWhatsapp verificacion = verificacionRepository.findByUsuarioId(usuarioId)
                .map(existente -> {
                    existente.reenviar(telefono, codigo, ahora);
                    return existente;
                })
                .orElseGet(() -> VerificacionWhatsapp.nueva(usuarioId, telefono, codigo, ahora));
        return verificacionRepository.save(verificacion);
    }

    /** Si no se pudo entregar, el código no sirve de nada: se descarta para no gastar un envío. */
    private void enviar(VerificacionWhatsapp verificacion, TelefonoWhatsapp telefono, String codigo) {
        if (!mensajeria.enviarTexto(telefono.valor(), TextosWhatsapp.codigoVerificacion(codigo))) {
            transaccionPort.ejecutar(() -> verificacionRepository.deleteByUsuarioId(verificacion.getUsuarioId()));
            throw new BusinessRuleException("No pudimos mandarte el código por WhatsApp. "
                    + "Revisá que el número tenga WhatsApp y que incluya el código de país");
        }
    }

    private static String nuevoCodigo() {
        return "%06d".formatted(ALEATORIO.nextInt(1_000_000));
    }
}
