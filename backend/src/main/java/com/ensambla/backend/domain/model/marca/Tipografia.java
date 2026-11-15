package com.ensambla.backend.domain.model.marca;

/** Las tipografías que puede elegir el negocio: una lista cerrada, así el cartel no pierde armonía. */
public enum Tipografia {

    PLUS_JAKARTA_SANS("Plus Jakarta Sans"),
    MONTSERRAT("Montserrat"),
    POPPINS("Poppins"),
    PLAYFAIR_DISPLAY("Playfair Display"),
    ROBOTO_SLAB("Roboto Slab"),
    NUNITO("Nunito");

    private final String familia;

    Tipografia(String familia) {
        this.familia = familia;
    }

    public String familia() {
        return familia;
    }
}
