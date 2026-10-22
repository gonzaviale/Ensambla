package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.dto.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.enums.TipoContenidoPlan;
import com.ensambla.backend.domain.repository.plan.AccesoContenidoRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccesoContenidoService {

    private static final int LIMITE_GRATUITO = 3;

    private final ControlPlanService controlPlanService;

    private final UsoPlanRepository usoPlanRepository;

    private final AccesoContenidoRepository accesoContenidoRepository;

    @Transactional
    public AccesoContenidoItem seleccionar(UUID negocioId, TipoContenidoPlan tipo, List<String> claves,
                                          OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        LocalDate periodo = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        OffsetDateTime fin = periodo.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<String> candidatas = claves.stream().distinct().toList();
        if (controlPlanService.esPremium(negocioId, ahora)) {
            return new AccesoContenidoItem(candidatas, 0, null, candidatas.size(), 0, fin);
        }
        Set<String> conocidas = accesoContenidoRepository.autorizadas(negocioId, tipo, periodo);
        List<String> permitidas = new ArrayList<>();
        for (String clave : candidatas) {
            if (conocidas.contains(clave)) {
                permitidas.add(clave);
            } else if (conocidas.size() < LIMITE_GRATUITO) {
                accesoContenidoRepository.registrar(negocioId, tipo, periodo, clave, ahora);
                conocidas.add(clave);
                permitidas.add(clave);
            }
        }
        return new AccesoContenidoItem(List.copyOf(permitidas), conocidas.size(), LIMITE_GRATUITO,
                candidatas.size(), candidatas.size() - permitidas.size(), fin);
    }

    @Transactional
    public void exigir(UUID negocioId, TipoContenidoPlan tipo, String clave, OffsetDateTime ahora) {
        if (!seleccionar(negocioId, tipo, List.of(clave), ahora).autorizadas().contains(clave)) {
            throw new RestriccionPlanException("LIMITE_CONTENIDO_" + tipo.name(),
                    "Usaste los tres contenidos incluidos este mes. Con Premium podés acceder a todos desde Planes.");
        }
    }
}
