package com.ensambla.backend.infrastructure.whatsapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Comprueba que quien llama al webhook es Meta: el token al registrarlo y la firma de cada aviso. */
@Component
public class VerificadorWebhookMeta {

    static final String ALGORITMO = "HmacSHA256";

    private static final String MODO_SUSCRIPCION = "subscribe";

    private static final String PREFIJO_FIRMA = "sha256=";

    private final WhatsappProperties whatsappProperties;

    private final String algoritmo;

    @Autowired
    public VerificadorWebhookMeta(WhatsappProperties whatsappProperties) {
        this(whatsappProperties, ALGORITMO);
    }

    VerificadorWebhookMeta(WhatsappProperties whatsappProperties, String algoritmo) {
        this.whatsappProperties = whatsappProperties;
        this.algoritmo = algoritmo;
    }

    /** Meta confirma la URL con el token que se cargó en su panel. Sin token configurado no acepta ninguno. */
    public boolean suscripcionValida(String modo, String token) {
        String esperado = whatsappProperties.verifyToken();
        return !esperado.isBlank() && MODO_SUSCRIPCION.equals(modo) && esperado.equals(token);
    }

    /**
     * Meta manda en {@code X-Hub-Signature-256} el HMAC-SHA256 del cuerpo, con el secreto de la app, en hexadecimal.
     * Sin secreto configurado no acepta ninguna firma.
     */
    public boolean firmaValida(byte[] cuerpo, String firma) {
        String secreto = whatsappProperties.appSecret();
        if (secreto.isBlank() || firma == null || !firma.startsWith(PREFIJO_FIRMA)) {
            return false;
        }
        byte[] esperada = (PREFIJO_FIRMA + HexFormat.of().formatHex(hmac(secreto, cuerpo)))
                .getBytes(StandardCharsets.US_ASCII);
        // Comparación de tiempo constante: no deja adivinar la firma midiendo cuánto tarda la respuesta.
        return MessageDigest.isEqual(esperada, firma.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] hmac(String secreto, byte[] cuerpo) {
        try {
            Mac mac = Mac.getInstance(algoritmo);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), algoritmo));
            return mac.doFinal(cuerpo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook de WhatsApp", e);
        }
    }
}
