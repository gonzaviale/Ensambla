package com.ensambla.backend.domain.model.objetivo;

import java.util.Optional;

/**
 * Una acción sin números que la persona hace por su cuenta y después responde: el texto de un mini-objetivo manual.
 * Una acción puede decir «hablá con 3 clientes», pero no afirmar una cifra de plata ni un porcentaje: eso es un dato
 * que sólo el servidor calcula.
 */
public record AccionManual(String titulo, String justificacion) {

    public static final int LARGO_MAXIMO_TITULO = 80;

    public static final int LARGO_MAXIMO_JUSTIFICACION = 280;

    private static final int LARGO_DEL_OBJETIVO_EN_EL_SEGUIMIENTO = 200;

    /** La acción con su texto limpio; vacía si falta el título o la justificación, pasan del largo o traen $ o %. */
    public static Optional<AccionManual> de(String titulo, String justificacion) {
        if (invalido(titulo, LARGO_MAXIMO_TITULO) || invalido(justificacion, LARGO_MAXIMO_JUSTIFICACION)) {
            return Optional.empty();
        }
        return Optional.of(new AccionManual(titulo.strip(), justificacion.strip()));
    }

    /** El seguimiento que se arma sin IA: una vez por semana la persona cuenta cómo va con su objetivo. */
    public static AccionManual seguimiento(String textoDelObjetivo) {
        String objetivo = textoDelObjetivo.strip();
        if (objetivo.length() > LARGO_DEL_OBJETIVO_EN_EL_SEGUIMIENTO) {
            objetivo = objetivo.substring(0, LARGO_DEL_OBJETIVO_EN_EL_SEGUIMIENTO).strip() + "…";
        }
        return new AccionManual(
                "Anotá cómo te fue con tu objetivo",
                "Una vez por semana contale a Sami cómo avanzaste con: " + objetivo);
    }

    private static boolean invalido(String texto, int largoMaximo) {
        return texto == null
                || texto.isBlank()
                || texto.strip().length() > largoMaximo
                || texto.contains("$")
                || texto.contains("%");
    }
}
