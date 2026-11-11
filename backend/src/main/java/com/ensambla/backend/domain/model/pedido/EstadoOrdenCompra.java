package com.ensambla.backend.domain.model.pedido;

/** En qué etapa gestiona el comercio una orden de compra. Una orden nueva espera que el dueño la acepte o la rechace. */
public enum EstadoOrdenCompra {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    EN_PREPARACION,
    LISTA
}
