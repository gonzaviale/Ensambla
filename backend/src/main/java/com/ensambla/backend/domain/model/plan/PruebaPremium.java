package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.time.OffsetDateTime;

public record PruebaPremium(UsuarioId usuarioId, OffsetDateTime inicioEn, OffsetDateTime finEn, int paginasIa) {

    public static final int LIMITE_PAGINAS_IA = 10;

    public static PruebaPremium nueva(UsuarioId usuarioId, OffsetDateTime ahora) {
        return new PruebaPremium(usuarioId, ahora, ahora.plusDays(7), 0);
    }

    public void validarPaginas(int cantidad) {
        if (cantidad > LIMITE_PAGINAS_IA - paginasIa) {
            throw new RestriccionPlanException("LIMITE_IA_PRUEBA",
                    "La prueba incluye diez análisis de foto en total. Tus datos se conservan.");
        }
    }

    public PruebaPremium consumir(int cantidad) {
        validarPaginas(cantidad);
        return new PruebaPremium(usuarioId, inicioEn, finEn, paginasIa + cantidad);
    }
}
