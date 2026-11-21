package com.ensambla.backend.domain.repository.metrica;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.util.List;
import java.util.Optional;

public interface MetricaRepository {

    /** Las del cuaderno, en el orden del panel. */
    List<Metrica> findAllByProyectoIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId);

    /** Una métrica de otro negocio o de otro cuaderno no se encuentra, igual que una que no existe. */
    Optional<Metrica> findByIdAndProyectoIdAndNegocioId(
            MetricaId metricaId, ProyectoId proyectoId, NegocioId negocioId);

    Metrica save(Metrica metrica);

    void deleteByIdAndNegocioId(MetricaId metricaId, NegocioId negocioId);

    default Metrica obtener(NegocioId negocioId, ProyectoId proyectoId, MetricaId metricaId) {
        return findByIdAndProyectoIdAndNegocioId(metricaId, proyectoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Métrica no encontrada"));
    }
}
