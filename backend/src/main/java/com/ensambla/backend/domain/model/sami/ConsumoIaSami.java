package com.ensambla.backend.domain.model.sami;

/**
 * Lo que costó una consulta a la IA: el modelo (o los modelos, si hubo que escalar) y los tokens gastados.
 */
public record ConsumoIaSami(String proveedor, String modelo, long tokensEntrada, long tokensSalida) {

    public ConsumoIaSami sumar(ConsumoIaSami otro) {
        return new ConsumoIaSami(
                proveedor,
                modelo + " + " + otro.modelo,
                tokensEntrada + otro.tokensEntrada,
                tokensSalida + otro.tokensSalida);
    }
}
