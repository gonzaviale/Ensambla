package com.ensambla.backend.domain.model.cartel;

/** El archivo en que se baja un cartel: PDF para imprimir y PNG para compartir en redes. */
public enum SalidaDeCartel {

    PDF("application/pdf", "pdf"),
    PNG("image/png", "png");

    private final String tipoMime;
    private final String extension;

    SalidaDeCartel(String tipoMime, String extension) {
        this.tipoMime = tipoMime;
        this.extension = extension;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public String extension() {
        return extension;
    }
}
