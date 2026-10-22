package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PruebaPremium(UUID negocioId, OffsetDateTime inicioEn, OffsetDateTime finEn, int paginasIa) {

    public static final int LIMITE_PAGINAS_IA = 10;

    public static PruebaPremium nueva(UUID negocioId, OffsetDateTime ahora) {
        return new PruebaPremium(negocioId, ahora, ahora.plusDays(7), 0);
    }

    public void validarPaginas(int cantidad) {
        if (cantidad > LIMITE_PAGINAS_IA - paginasIa) {
            throw new RestriccionPlanException("LIMITE_IA_PRUEBA",
                    "La prueba incluye diez páginas de IA en total. Tus datos se conservan.");
        }
    }

    public PruebaPremium consumir(int cantidad) {
        validarPaginas(cantidad);
        return new PruebaPremium(negocioId, inicioEn, finEn, paginasIa + cantidad);
    }
}
