package com.ensambla.backend.domain.service.procesamiento;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ProcesamientoService {

    private final ProcesamientoRepository procesamientoRepository;

    public EstadoLectura obtener(NegocioId negocioId, LoteId loteId) {
        return procesamientoRepository.obtener(negocioId, loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    public ConsumoDiario consumo(NegocioId negocioId, OffsetDateTime ahora) {
        return procesamientoRepository.consumo(negocioId, ahora.toLocalDate());
    }
}
