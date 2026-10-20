package com.ensambla.backend.infrastructure.ia;

/** Pedido de texto a texto al modelo: instrucciones fijas, contexto del caso y esquema del JSON esperado. */
public record PeticionModeloTexto(String instrucciones, String contexto, String esquemaJson) {
}
