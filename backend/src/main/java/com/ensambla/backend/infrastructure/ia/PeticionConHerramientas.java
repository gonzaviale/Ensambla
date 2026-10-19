package com.ensambla.backend.infrastructure.ia;

/** Pedido al modelo con herramientas disponibles: instrucciones fijas y el contexto del turno. */
public record PeticionConHerramientas(String instrucciones, String contexto) {
}
