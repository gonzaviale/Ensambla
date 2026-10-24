package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ProcesamientoService {

    private final ProcesamientoRepository procesamientoRepository;

    public EstadoLectura obtener(UUID negocioId, UUID loteId) {
        return procesamientoRepository.obtener(negocioId, loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    public ConsumoDiario consumo(UUID negocioId, OffsetDateTime ahora) {
        return procesamientoRepository.consumo(negocioId, ahora.toLocalDate());
    }
}
