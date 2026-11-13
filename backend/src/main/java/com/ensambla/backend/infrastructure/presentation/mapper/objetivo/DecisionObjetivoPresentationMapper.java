package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecidirPasoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionesObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionesObjetivoResponse.SemanaDecisionesResponse;
import org.springframework.stereotype.Component;

@Component
public class DecisionObjetivoPresentationMapper {

    public RegistrarDecisionPasoRequest toRegistrarRequest(
            UsuarioId usuarioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId, DecidirPasoWebRequest request) {
        return new RegistrarDecisionPasoRequest(usuarioId, objetivoId, miniObjetivoId, request.tipo());
    }

    public ListarDecisionesRequest toListarRequest(UsuarioId usuarioId, ObjetivoId objetivoId) {
        return new ListarDecisionesRequest(usuarioId, objetivoId);
    }

    public DecisionesObjetivoResponse toResponse(ListarDecisionesResponse response) {
        return new DecisionesObjetivoResponse(response.semanas().stream()
                .map(semana -> new SemanaDecisionesResponse(
                        semana.semanaInicio(), semana.decisiones().stream().map(this::toResponse).toList()))
                .toList());
    }

    public DecisionObjetivoResponse toResponse(DecisionItem item) {
        return new DecisionObjetivoResponse(
                item.id(),
                item.tipo(),
                item.miniObjetivoId(),
                item.avisoId(),
                item.registroId(),
                item.descripcion(),
                item.tomadaEn(),
                item.semanaInicio(),
                item.efecto(),
                item.deltaIngresoSemanal());
    }
}
