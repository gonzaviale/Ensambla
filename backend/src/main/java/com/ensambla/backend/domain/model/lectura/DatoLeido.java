package com.ensambla.backend.domain.model.lectura;

/**
 * Un valor leído de un renglón.
 *
 * @param valor      texto tal como hay que guardarlo (fechas ISO, números con punto decimal); {@code null} si está vacío
 * @param confianza  de 0 a 1, qué tan segura está la IA de la lectura
 */
public record DatoLeido(String valor, double confianza) {

    /** Un dato es dudoso cuando la confianza no llega al umbral; la pantalla de revisión lo resalta (T-102). */
    public boolean esDudoso(double umbral) {
        return confianza < umbral;
    }
}
