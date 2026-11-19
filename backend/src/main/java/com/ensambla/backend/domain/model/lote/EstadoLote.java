package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Ciclo de vida de un lote de fotos: se sube, la IA lo lee y el usuario lo confirma o descarta. */
public enum EstadoLote {
    PENDIENTE,
    PROCESANDO,
    LISTO {
        @Override
        public void exigirQueSePuedaConfirmar() {
            // una lectura terminada se puede confirmar
        }
    },
    ERROR,
    CONFIRMADO {
        @Override
        public void exigirQueSePuedaConfirmar() {
            throw new BusinessRuleException("Este cuaderno ya se guardó");
        }
    },
    DESCARTADO;

    /**
     * Sólo una lectura terminada se puede confirmar, y una sola vez.
     *
     * @throws BusinessRuleException si el lote todavía se está leyendo, falló o ya se guardó
     */
    public void exigirQueSePuedaConfirmar() {
        throw new BusinessRuleException("Este cuaderno todavía no terminó de leerse");
    }
}
