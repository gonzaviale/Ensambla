package com.ensambla.backend.infrastructure.presentation.mapper.procesamiento;

import com.ensambla.backend.domain.dto.procesamiento.ConsumoItem;
import com.ensambla.backend.domain.dto.procesamiento.LecturaItem;
import com.ensambla.backend.domain.dto.procesamiento.UsuarioLoteRequest;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.ConsumoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.procesamiento.LecturaResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LecturaPresentationMapper {

    public UsuarioLoteRequest toRequest(UUID usuarioId, UUID loteId) {
        return new UsuarioLoteRequest(usuarioId, loteId);
    }

    public LecturaResponse toResponse(LecturaItem item) {
        return new LecturaResponse(item.loteId(), item.estado(), item.motivoError(), item.intentos(), item.propuesta());
    }

    public ConsumoResponse toResponse(ConsumoItem item) {
        return new ConsumoResponse(item.fecha(), item.limite(), item.paginas(), item.reservadas(), item.disponibles(),
                item.tokensEntrada(), item.tokensSalida(), item.costoEstimadoUsd(), item.consumoCompleto());
    }
}
