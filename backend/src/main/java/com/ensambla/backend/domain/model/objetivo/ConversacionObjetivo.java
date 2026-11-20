package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.util.List;

/**
 * La conversación con Sami para armar un objetivo, tal como llega: lo que la persona escribió y lo que Sami le
 * contestó antes. El texto de la persona es un dato: se acota (hasta {@link #LARGO_MAXIMO_MENSAJE} caracteres por
 * mensaje y {@code maximoMensajes} mensajes suyos) y se limpia antes de mandarlo a la IA, nunca se obedece.
 *
 * @param mensajes       sin espacios en los extremos; el último es de la persona
 * @param maximoMensajes cuántos mensajes de la persona admite la conversación
 */
public record ConversacionObjetivo(List<MensajeObjetivo> mensajes, int maximoMensajes) {

    public static final int LARGO_MAXIMO_MENSAJE = 500;

    /**
     * @throws BusinessRuleException si no hay mensajes, uno está vacío o es muy largo, el último no es de la persona o
     *                               la persona ya escribió el máximo
     */
    public static ConversacionObjetivo de(List<MensajeObjetivo> mensajes, int maximoMensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            throw new BusinessRuleException("Contale a Sami qué querés lograr");
        }
        List<MensajeObjetivo> limpios = mensajes.stream().map(ConversacionObjetivo::limpio).toList();
        if (limpios.getLast().rol() != RolMensaje.PERSONA) {
            throw new BusinessRuleException("El último mensaje tiene que ser tuyo");
        }
        ConversacionObjetivo conversacion = new ConversacionObjetivo(limpios, maximoMensajes);
        if (conversacion.mensajesDeLaPersona() > maximoMensajes) {
            throw new BusinessRuleException("Llegaste al máximo de " + maximoMensajes
                    + " mensajes de esta conversación. Guardá lo que armamos o empezá una nueva");
        }
        return conversacion;
    }

    private static MensajeObjetivo limpio(MensajeObjetivo mensaje) {
        if (mensaje.texto() == null || mensaje.texto().isBlank()) {
            throw new BusinessRuleException("Los mensajes no pueden estar vacíos");
        }
        String texto = mensaje.texto().strip();
        if (texto.length() > LARGO_MAXIMO_MENSAJE) {
            throw new BusinessRuleException(
                    "Cada mensaje puede tener hasta " + LARGO_MAXIMO_MENSAJE + " caracteres");
        }
        return new MensajeObjetivo(mensaje.rol(), texto);
    }

    /** Cuántos mensajes más puede escribir la persona. */
    public int mensajesRestantes() {
        return maximoMensajes - mensajesDeLaPersona();
    }

    /** Lo primero que la persona le contó a Sami: su objetivo, con sus palabras. */
    public String objetivoDeLaPersona() {
        return mensajes.stream()
                .filter(mensaje -> mensaje.rol() == RolMensaje.PERSONA)
                .findFirst()
                .orElseThrow()
                .texto();
    }

    /** Los mensajes como se le mandan a la IA: sin datos personales. */
    public List<MensajeObjetivo> paraLaIa() {
        return mensajes.stream()
                .map(mensaje -> new MensajeObjetivo(mensaje.rol(), TextoSeguro.de(mensaje.texto())))
                .toList();
    }

    private int mensajesDeLaPersona() {
        return (int) mensajes.stream().filter(mensaje -> mensaje.rol() == RolMensaje.PERSONA).count();
    }
}
