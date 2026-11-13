package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.List;

/** Las alternativas para cambiar un mini-objetivo, de la que más suma a la que menos. */
public record ListarCandidatosResponse(List<CandidatoItem> content) {

    public static ListarCandidatosResponse de(List<CandidatoMiniObjetivo> candidatos) {
        return new ListarCandidatosResponse(candidatos.stream().map(CandidatoItem::de).toList());
    }

    /** Lo que hay que mandar de vuelta ({@code tipo}, {@code registroId}, {@code referencia}) para elegirlo. */
    public record CandidatoItem(
            TipoMiniObjetivo tipo,
            SentidoMiniObjetivo sentido,
            String titulo,
            String justificacion,
            String referencia,
            RegistroId registroId,
            BigDecimal valorInicial,
            BigDecimal meta,
            BigDecimal aporteEstimado) {

        private static CandidatoItem de(CandidatoMiniObjetivo candidato) {
            return new CandidatoItem(
                    candidato.tipo(),
                    candidato.tipo().sentido(),
                    candidato.titulo(),
                    candidato.justificacion(),
                    candidato.referencia(),
                    candidato.registroId(),
                    candidato.valorInicial(),
                    candidato.meta(),
                    candidato.aporteEstimado());
        }
    }
}
