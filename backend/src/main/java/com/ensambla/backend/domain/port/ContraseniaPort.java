package com.ensambla.backend.domain.port;

public interface ContraseniaPort {

    String hashear(String contrasenia);

    boolean coincide(String contrasenia, String hash);
}

