package com.ensambla.backend.domain.model.marca;

/** Cómo habla el negocio. Lo lee quien redacta los textos; no cambia el diseño. */
public enum TonoDeMarca {

    CERCANO("cercano y cálido, como un vecino de confianza"),
    PROFESIONAL("claro y formal, sin exclamaciones"),
    DIVERTIDO("alegre y canchero, con humor liviano");

    private final String indicacion;

    TonoDeMarca(String indicacion) {
        this.indicacion = indicacion;
    }

    public String indicacion() {
        return indicacion;
    }
}
