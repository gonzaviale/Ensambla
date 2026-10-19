package com.ensambla.backend.infrastructure.whatsapp;

import com.ensambla.backend.domain.port.GeneradorCodigoPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Código de 6 dígitos con un generador criptográfico: no se puede adivinar a partir de los anteriores. */
@Component
public class GeneradorCodigoAleatorioAdapter implements GeneradorCodigoPort {

    private static final int COMBINACIONES = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generar() {
        return String.format("%06d", random.nextInt(COMBINACIONES));
    }
}
