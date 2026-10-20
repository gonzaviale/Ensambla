package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.procesamiento.PoliticaProcesamiento;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Registra el intento antes de salir al proveedor; un fallo o timeout también puede tener costo. */
@Service
@RequiredArgsConstructor
public class ConsumoIaInmediatoService {

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    private final PoliticaProcesamiento politicaProcesamiento;

    @Transactional
    public void registrar(UUID negocioId, OffsetDateTime ahora) {
        controlPlanService.validarPaginasIa(negocioId, 1, ahora);
        LocalDate dia = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
        if (!usoPlanRepository.registrarPaginaInmediata(
                negocioId, dia, politicaProcesamiento.limiteDiario(), ahora)) {
            throw new RestriccionPlanException("LIMITE_IA_DIARIO",
                    "Se alcanzó el límite diario de páginas de IA. Probá mañana.");
        }
        controlPlanService.registrarPaginasPrueba(negocioId, 1, ahora);
    }
}
