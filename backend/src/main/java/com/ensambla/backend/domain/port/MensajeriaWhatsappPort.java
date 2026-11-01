package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;

/** Salida hacia WhatsApp: el dominio no sabe qué proveedor entrega los mensajes. */
public interface MensajeriaWhatsappPort {

    /**
     * Responde por el chat al número indicado (sólo dígitos, con código de país).
     *
     * @return {@code false} si el proveedor no pudo entregarlo
     */
    boolean enviarTexto(String telefono, String texto);

    /**
     * La foto de un mensaje recibido, tal como la guardó el proveedor.
     *
     * @throws RuntimeException si el proveedor no la tiene o no responde
     */
    ArchivoWhatsapp descargarMedia(String idExterno);

    /** Número del bot al que el usuario le escribe; vacío si no se configuró. */
    String numeroBot();
}
