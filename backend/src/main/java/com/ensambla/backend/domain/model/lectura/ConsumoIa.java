package com.ensambla.backend.domain.model.lectura;

/** Lo que costó una lectura, para registrar el consumo por negocio (T-098). */
public record ConsumoIa(String proveedor, String modelo, String versionPrompt, int paginas,
                        int tokensEntrada, int tokensSalida, long duracionMs) {
}
