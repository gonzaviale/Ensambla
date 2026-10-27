package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * Código de 6 dígitos que Ensambla manda por WhatsApp al número que la persona cargó en la app. Sólo se guarda su
 * huella; los límites de intentos y de reenvíos evitan que se adivine o que se use para mandar mensajes de más.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VerificacionWhatsapp {

    public static final Duration VIGENCIA = Duration.ofMinutes(10);
    public static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);
    public static final Duration VENTANA_ENVIOS = Duration.ofHours(1);
    public static final int MAX_INTENTOS = 5;
    public static final int MAX_ENVIOS_POR_VENTANA = 5;

    private VerificacionWhatsappId id;

    private UsuarioId usuarioId;

    private String telefono;

    private String codigoHash;

    private int intentos;

    private OffsetDateTime enviadoEn;

    private OffsetDateTime expiraEn;

    private int enviosVentana;

    private OffsetDateTime ventanaDesde;

    public static VerificacionWhatsapp nueva(UsuarioId usuarioId, TelefonoWhatsapp telefono, String codigo,
                                             OffsetDateTime ahora) {
        return VerificacionWhatsapp.builder()
                .usuarioId(usuarioId)
                .telefono(telefono.valor())
                .codigoHash(huella(codigo))
                .enviadoEn(ahora)
                .expiraEn(ahora.plus(VIGENCIA))
                .enviosVentana(1)
                .ventanaDesde(ahora)
                .build();
    }

    /** Reemplaza el código por uno nuevo (y quizá de otro número) si pasó la espera y no se agotaron los envíos. */
    public void reenviar(TelefonoWhatsapp nuevoTelefono, String codigo, OffsetDateTime ahora) {
        if (ahora.isBefore(reenviarDesde())) {
            throw new BusinessRuleException("Esperá un momento antes de pedir otro código");
        }
        if (!ahora.isBefore(ventanaDesde.plus(VENTANA_ENVIOS))) {
            ventanaDesde = ahora;
            enviosVentana = 0;
        }
        if (enviosVentana >= MAX_ENVIOS_POR_VENTANA) {
            throw new BusinessRuleException("Pediste demasiados códigos. Probá de nuevo en una hora");
        }
        enviosVentana++;
        telefono = nuevoTelefono.valor();
        codigoHash = huella(codigo);
        intentos = 0;
        enviadoEn = ahora;
        expiraEn = ahora.plus(VIGENCIA);
    }

    /** Desde cuándo se puede pedir otro código. */
    public OffsetDateTime reenviarDesde() {
        return enviadoEn.plus(ESPERA_REENVIO);
    }

    /** Cuenta el intento antes de comparar: quien llama debe guardar la verificación aunque esto falle. */
    public void confirmar(String codigoIngresado, OffsetDateTime ahora) {
        if (!ahora.isBefore(expiraEn)) {
            throw new BusinessRuleException("El código venció. Pedí uno nuevo");
        }
        if (intentos >= MAX_INTENTOS) {
            throw new BusinessRuleException("Hiciste demasiados intentos. Pedí un código nuevo");
        }
        intentos++;
        boolean coincide = MessageDigest.isEqual(huella(codigoIngresado.trim()).getBytes(StandardCharsets.UTF_8),
                codigoHash.getBytes(StandardCharsets.UTF_8));
        if (!coincide) {
            throw new BusinessRuleException("El código no es correcto. Te quedan %d intentos"
                    .formatted(MAX_INTENTOS - intentos));
        }
    }

    /** SHA-256 en hexadecimal. */
    public static String huella(String codigo) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
