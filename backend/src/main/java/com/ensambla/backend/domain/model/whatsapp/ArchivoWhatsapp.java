package com.ensambla.backend.domain.model.whatsapp;

/** Archivo (una foto) que el proveedor tenía guardado de un mensaje. */
public record ArchivoWhatsapp(byte[] contenido, String mime) {
}
