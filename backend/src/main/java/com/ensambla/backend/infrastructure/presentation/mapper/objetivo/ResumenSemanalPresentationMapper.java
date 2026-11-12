package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.MiniItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ReajusteItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.DecisionesSemanaResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.MiniReajusteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.ReajusteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.SemanaResponse;
import org.springframework.stereotype.Component;

@Component
public class ResumenSemanalPresentationMapper {

    public ListarSemanasRequest toRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
        return new ListarSemanasRequest(usuarioId, objetivoId);
    }

    public SemanasObjetivoResponse toResponse(ListarSemanasResponse response) {
        return new SemanasObjetivoResponse(
                response.ultimoCierre(),
                response.semanas().stream().map(this::toResponse).toList());
    }

    private SemanaResponse toResponse(SemanaItem semana) {
        return new SemanaResponse(
                semana.semana(),
                semana.resultado(),
                semana.avance(),
                new DecisionesSemanaResponse(
                        semana.decisiones().aplicadas(),
                        semana.decisiones().descartadas(),
                        semana.decisiones().aceptadas(),
                        semana.decisiones().ignoradas()),
                semana.queFunciono(),
                semana.textoConIa(),
                semana.reajustes().stream().map(this::toResponse).toList());
    }

    private ReajusteResponse toResponse(ReajusteItem reajuste) {
        return new ReajusteResponse(reajuste.tipo(), reajuste.motivo(), toResponse(reajuste.de()),
                reajuste.a() == null ? null : toResponse(reajuste.a()));
    }

    private MiniReajusteResponse toResponse(MiniItem mini) {
        return new MiniReajusteResponse(mini.titulo(), mini.meta());
    }
}
