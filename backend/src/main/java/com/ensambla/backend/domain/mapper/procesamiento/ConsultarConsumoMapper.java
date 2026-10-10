package com.ensambla.backend.domain.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConsumoItem;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;

public class ConsultarConsumoMapper {

    public ConsumoItem toResponse(ConsumoDiario consumo, int limite) {
        return new ConsumoItem(consumo.fecha(), limite, consumo.paginas(), consumo.reservadas(),
                Math.max(0, limite - consumo.paginas() - consumo.reservadas()),
                consumo.tokensEntrada(), consumo.tokensSalida(), consumo.costoEstimadoUsd(), consumo.consumoCompleto());
    }
}
