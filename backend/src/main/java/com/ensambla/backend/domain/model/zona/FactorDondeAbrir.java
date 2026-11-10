package com.ensambla.backend.domain.model.zona;

public enum FactorDondeAbrir {
    COMPETENCIA("Poca competencia", true),
    ACTIVIDAD_ACCESO("Actividad y acceso estimados", false),
    DENSIDAD("Vecinos cerca", false),
    ESCUELAS("Escuelas cerca", false),
    OFICINAS("Oficinas cerca", false),
    TRANSPORTE("Transporte", false),
    PARQUES("Parques cerca", false),
    SALUD("Salud cerca", false),
    COMPLEMENTARIOS("Comercios que atraen gente", false),
    PERFIL_SOCIOECONOMICO("Poder de compra", false),
    DEMANDA_ENSAMBLA("Demanda observada en Ensambla", false),
    ALQUILER("Alquiler accesible", true);

    private final String etiqueta;
    private final boolean menosEsMejor;

    FactorDondeAbrir(String etiqueta, boolean menosEsMejor) {
        this.etiqueta = etiqueta;
        this.menosEsMejor = menosEsMejor;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public boolean menosEsMejor() {
        return menosEsMejor;
    }
}
