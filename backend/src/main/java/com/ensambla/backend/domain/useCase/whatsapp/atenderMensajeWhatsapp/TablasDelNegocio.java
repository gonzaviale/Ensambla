package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Las tablas del negocio donde se puede cargar un dato desde el chat. */
@RequiredArgsConstructor
final class TablasDelNegocio {

    private final ProcesamientoRepository procesamientoRepository;

    List<TablaDisponible> de(NegocioId negocioId) {
        List<TablaDisponible> tablas = buscarTablas(negocioId);
        exigirQueHayaTablas(tablas);
        return tablas;
    }

    static TablaDisponible tablaDe(List<TablaDisponible> tablas, BloqueId bloqueId) {
        return tablas.stream()
                .filter(tabla -> tabla.bloque().id().equals(bloqueId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(TextosWhatsapp.TABLA_INEXISTENTE));
    }

    static List<BloqueLectura> bloquesDe(List<TablaDisponible> tablas) {
        return tablas.stream().map(TablaDisponible::bloque).toList();
    }

    private List<TablaDisponible> buscarTablas(NegocioId negocioId) {
        return procesamientoRepository.tablas(negocioId);
    }

    private static void exigirQueHayaTablas(List<TablaDisponible> tablas) {
        if (tablas.isEmpty()) {
            throw new BusinessRuleException(TextosWhatsapp.SIN_TABLAS);
        }
    }
}
