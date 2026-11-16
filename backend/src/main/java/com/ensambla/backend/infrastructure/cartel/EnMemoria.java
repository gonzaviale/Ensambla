package com.ensambla.backend.infrastructure.cartel;

/** Para escribir archivos en memoria: los errores de los APIs de imagen y PDF son sólo formales y acá no pasan. */
final class EnMemoria {

    /** Un trabajo de los APIs de imagen o PDF, que declaran excepciones verificadas. */
    @FunctionalInterface
    interface Trabajo<T> {

        T hacer() throws Exception;
    }

    private EnMemoria() {
    }

    static <T> T sinFallar(Trabajo<T> trabajo) {
        try {
            return trabajo.hacer();
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo armar el archivo del cartel", error);
        }
    }
}
