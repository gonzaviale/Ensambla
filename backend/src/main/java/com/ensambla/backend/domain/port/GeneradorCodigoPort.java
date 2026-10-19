package com.ensambla.backend.domain.port;

/** Genera los códigos de 6 dígitos con los que una persona vincula su número de WhatsApp. */
public interface GeneradorCodigoPort {

    String generar();
}
