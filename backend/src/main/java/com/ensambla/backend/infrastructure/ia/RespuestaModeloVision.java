package com.ensambla.backend.infrastructure.ia;

/**
 * Respuesta cruda del modelo: el JSON todavía sin validar y el uso de tokens que informó el proveedor.
 */
public record RespuestaModeloVision(String json, String proveedor, String modelo, int tokensEntrada,
                                    int tokensSalida) {
}
