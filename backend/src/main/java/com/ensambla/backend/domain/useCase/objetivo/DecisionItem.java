package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.EfectoDecision;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Una decisión de la persona con su efecto sobre las ventas.
 *
 * @param deltaIngresoSemanal lo que cambió el ingreso semanal del producto; sólo si el efecto está medido
 */
public record DecisionItem(
        DecisionObjetivoId id,
        TipoDecision tipo,
        MiniObjetivoId miniObjetivoId,
        AvisoId avisoId,
        RegistroId registroId,
        String descripcion,
        OffsetDateTime tomadaEn,
        LocalDate semanaInicio,
        EstadoEfecto efecto,
        BigDecimal deltaIngresoSemanal) {

    public static DecisionItem de(DecisionObjetivo decision, EfectoDecision efecto) {
        return new DecisionItem(
                decision.id(),
                decision.tipo(),
                decision.miniObjetivoId(),
                decision.avisoId(),
                decision.registroId(),
                decision.descripcion(),
                decision.tomadaEn(),
                decision.semanaInicio(),
                efecto.estado(),
                efecto.deltaIngresoSemanal());
    }
}
