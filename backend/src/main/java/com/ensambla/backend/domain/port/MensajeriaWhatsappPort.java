package com.ensambla.backend.domain.port;

/** Salida hacia WhatsApp: el dominio no sabe qué proveedor entrega los mensajes. */
public interface MensajeriaWhatsappPort {

    /** Responde por el chat al número indicado (sólo dígitos, con código de país). */
    void enviarTexto(String telefono, String texto);

    /** Número del bot al que el usuario le escribe; vacío si no se configuró. */
    String numeroBot();
}
