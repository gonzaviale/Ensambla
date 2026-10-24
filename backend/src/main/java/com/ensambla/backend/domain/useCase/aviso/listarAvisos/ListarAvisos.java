package com.ensambla.backend.domain.useCase.aviso.listarAvisos;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarAvisos {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final AccesoContenidoService accesoContenidoService;
    private final Clock clock;

    public List<AvisoItem> execute(ListarAvisosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        var avisos = avisoService.listar(negocioId, request.estado());
        var acceso = accesoContenidoService.seleccionar(negocioId, TipoContenidoPlan.AVISOS,
                avisos.stream().map(aviso -> "servidor:" + aviso.getId()).toList(), OffsetDateTime.now(clock));
        return avisos.stream()
                .filter(aviso -> acceso.autorizadas().contains("servidor:" + aviso.getId()))
                .map(AvisoItem::de)
                .toList();
    }
}
