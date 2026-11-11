package com.ensambla.backend.domain.model.pedido;

/**
 * En qué etapa gestiona el comercio una orden de compra. Una orden nueva espera que el dueño la acepte o la rechace;
 * una aceptada se prepara y queda lista para retirar. Rechazada y lista son finales.
 */
public enum EstadoOrdenCompra {
    PENDIENTE("pendiente"),
    ACEPTADA("aceptada"),
    RECHAZADA("rechazada"),
    EN_PREPARACION("en preparación"),
    LISTA("lista para retirar");

    private final String etiqueta;

    EstadoOrdenCompra(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Cómo se dice en una frase: «una orden pendiente». */
    public String etiqueta() {
        return etiqueta;
    }

    /** Si el dueño puede llevar una orden de este estado al otro. */
    public boolean puedePasarA(EstadoOrdenCompra destino) {
        return switch (this) {
            case PENDIENTE -> destino == ACEPTADA || destino == RECHAZADA;
            case ACEPTADA -> destino == EN_PREPARACION || destino == LISTA;
            case EN_PREPARACION -> destino == LISTA;
            case RECHAZADA, LISTA -> false;
        };
    }
}
