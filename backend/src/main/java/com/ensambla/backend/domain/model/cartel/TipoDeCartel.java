package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/**
 * Las plantillas de contenido: qué se anuncia, cuántas líneas de detalle admite y el texto de reserva que se usa
 * cuando no hay IA o su texto no sirve.
 */
public enum TipoDeCartel {

    PROMO_PRECIO(1, "¡Oferta en %s!", "Aprovechá este precio mientras dure"),
    DOS_POR_UNO(1, "2x1 en %s", "Llevás dos y pagás uno"),
    COMBO(4, "Combo con %s", "Juntos salen mejor"),
    NOVEDAD(1, "Llegó %s", "Ya podés pasar a verlo"),
    HORARIOS(7, "Nuestros horarios", "Te esperamos"),
    LISTA_PRECIOS(10, "Lista de precios", "Precios al día");

    private static final int LARGO_MAXIMO_DEL_PRODUCTO = 24;

    private final int maximoDeLineas;
    private final String titularDeReserva;
    private final String bajadaDeReserva;

    TipoDeCartel(int maximoDeLineas, String titularDeReserva, String bajadaDeReserva) {
        this.maximoDeLineas = maximoDeLineas;
        this.titularDeReserva = titularDeReserva;
        this.bajadaDeReserva = bajadaDeReserva;
    }

    public void exigirLineasAdmitidas(int cantidad) {
        if (cantidad > maximoDeLineas) {
            throw new BusinessRuleException("Un cartel de este tipo admite hasta " + maximoDeLineas + " líneas");
        }
    }

    public String titularDeReservaPara(String producto) {
        return titularDeReserva.formatted(abreviado(producto));
    }

    public String bajadaDeReserva() {
        return bajadaDeReserva;
    }

    private String abreviado(String producto) {
        return producto.length() <= LARGO_MAXIMO_DEL_PRODUCTO
                ? producto
                : producto.substring(0, LARGO_MAXIMO_DEL_PRODUCTO - 1).stripTrailing() + "…";
    }
}
