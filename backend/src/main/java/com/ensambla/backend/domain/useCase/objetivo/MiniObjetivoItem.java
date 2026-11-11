package com.ensambla.backend.domain.useCase.objetivo;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un mini-objetivo tal como lo ve la persona: con su avance de 0 a 1 ya calculado. */
public record MiniObjetivoItem(
        MiniObjetivoId id,
        int posicion,
        TipoMiniObjetivo tipo,
        SentidoMiniObjetivo sentido,
        EstadoMiniObjetivo estado,
        String titulo,
        String justificacion,
        String referencia,
        RegistroId registroId,
        BigDecimal valorInicial,
        BigDecimal valorActual,
        BigDecimal meta,
        BigDecimal avance,
        BigDecimal aporteEstimado,
        LocalDate semanaInicio,
        boolean textoConIa) {

    public static MiniObjetivoItem de(MiniObjetivo miniObjetivo) {
        return new MiniObjetivoItem(
                miniObjetivo.getId(),
                miniObjetivo.getPosicion(),
                miniObjetivo.getTipo(),
                miniObjetivo.getTipo().sentido(),
                miniObjetivo.getEstado(),
                miniObjetivo.getTitulo(),
                miniObjetivo.getJustificacion(),
                miniObjetivo.getReferencia(),
                miniObjetivo.getRegistroId(),
                miniObjetivo.getValorInicial(),
                miniObjetivo.getValorActual(),
                miniObjetivo.getMeta(),
                miniObjetivo.avance(),
                miniObjetivo.getAporteEstimado(),
                miniObjetivo.getSemanaInicio(),
                miniObjetivo.isTextoConIa());
    }
}
