package com.ensambla.backend.domain.useCase.aviso.listarAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class ListarAvisos {

    private static final Comparator<Aviso> PENDIENTES_PRIMERO_Y_MAS_URGENTES = Comparator
            .comparing((Aviso aviso) -> !aviso.estaPendiente())
            .thenComparing(Comparator.comparing(Aviso::getPrioridad).reversed())
            .thenComparing(Comparator.comparing(Aviso::getCreadoEn).reversed());

    private final NegocioRepository negocioRepository;
    private final AvisoRepository avisoRepository;
    private final ControlPlanService controlPlanService;
    private final Clock clock;

    public List<AvisoItem> execute(ListarAvisosRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        List<Aviso> avisos = obtenerAvisosOrdenados(negocioId, request.estado());
        Set<AvisoId> visibles = avisosVisiblesParaElPlan(negocioId, avisos);
        return avisos.stream()
                .filter(aviso -> visibles.contains(aviso.getId()))
                .map(AvisoItem::de)
                .toList();
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    /** Sin estado, todos los avisos del negocio; los pendientes van primero, del más urgente al menos urgente. */
    private List<Aviso> obtenerAvisosOrdenados(NegocioId negocioId, EstadoAviso estado) {
        List<Aviso> avisos = estado == null
                ? avisoRepository.findByNegocioId(negocioId)
                : avisoRepository.findByNegocioIdAndEstado(negocioId, estado);
        return avisos.stream().sorted(PENDIENTES_PRIMERO_Y_MAS_URGENTES).toList();
    }

    private Set<AvisoId> avisosVisiblesParaElPlan(NegocioId negocioId, List<Aviso> avisos) {
        List<AvisoId> ids = avisos.stream().map(Aviso::getId).toList();
        return Set.copyOf(controlPlanService.avisosAutorizados(negocioId, ids, OffsetDateTime.now(clock)));
    }
}
