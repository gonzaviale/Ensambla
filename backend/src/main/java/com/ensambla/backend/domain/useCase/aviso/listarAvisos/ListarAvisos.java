package com.ensambla.backend.domain.useCase.aviso.listarAvisos;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarAvisos {

    private static final Comparator<Aviso> PENDIENTES_PRIMERO_Y_MAS_URGENTES = Comparator
            .comparing((Aviso aviso) -> !aviso.estaPendiente())
            .thenComparing(Comparator.comparing(Aviso::getPrioridad).reversed())
            .thenComparing(Comparator.comparing(Aviso::getCreadoEn).reversed());

    private final NegocioService negocioService;
    private final AvisoRepository avisoRepository;
    private final AccesoContenidoService accesoContenidoService;
    private final Clock clock;

    public List<AvisoItem> execute(ListarAvisosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        List<Aviso> avisos = listar(negocioId, request.estado());
        var acceso = accesoContenidoService.seleccionar(negocioId, TipoContenidoPlan.AVISOS,
                avisos.stream().map(aviso -> "servidor:" + aviso.getId()).toList(), OffsetDateTime.now(clock));
        return avisos.stream()
                .filter(aviso -> acceso.autorizadas().contains("servidor:" + aviso.getId()))
                .map(AvisoItem::de)
                .toList();
    }

    /** Sin estado, todos los avisos del negocio; los pendientes van primero, del más urgente al menos urgente. */
    private List<Aviso> listar(UUID negocioId, EstadoAviso estado) {
        List<Aviso> avisos = estado == null
                ? avisoRepository.findByNegocioId(negocioId)
                : avisoRepository.findByNegocioIdAndEstado(negocioId, estado);
        return avisos.stream().sorted(PENDIENTES_PRIMERO_Y_MAS_URGENTES).toList();
    }
}
