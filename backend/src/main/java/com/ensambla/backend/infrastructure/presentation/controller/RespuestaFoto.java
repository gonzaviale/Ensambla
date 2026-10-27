package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

/** Arma la respuesta HTTP de una foto privada: sólo la persona de la sesión puede guardarla en su caché. */
final class RespuestaFoto {

    private static final Duration VIGENCIA = Duration.ofMinutes(5);

    private RespuestaFoto() {
    }

    static ResponseEntity<byte[]> de(FotoArchivoResponse foto) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CacheControl.maxAge(VIGENCIA).cachePrivate())
                .body(foto.contenido());
    }
}
