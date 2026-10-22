package com.ensambla.backend.infrastructure.ia;

/** Respuesta cruda del modelo: el JSON todavía sin validar y los tokens que informó el proveedor. */
public record RespuestaModeloTexto(String json, String proveedor, String modelo, long tokensEntrada,
                                   long tokensSalida) {
}
