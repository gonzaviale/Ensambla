package com.ensambla.backend.infrastructure.security;

import com.ensambla.backend.domain.port.ContraseniaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContraseniaAdapter implements ContraseniaPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashear(String contrasenia) {
        return passwordEncoder.encode(contrasenia);
    }

    @Override
    public boolean coincide(String contrasenia, String hash) {
        return passwordEncoder.matches(contrasenia, hash);
    }
}

