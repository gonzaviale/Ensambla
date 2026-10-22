package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.dto.aviso.AvisoItem;
import com.ensambla.backend.domain.dto.aviso.ListarAvisosRequest;
import com.ensambla.backend.domain.mapper.aviso.AvisoItemMapper;
import com.ensambla.backend.domain.model.enums.TipoContenidoPlan;
import com.ensambla.backend.domain.service.aviso.AvisoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.AccesoContenidoService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarAvisosUseCase {

    private final NegocioService negocioService;
    private final AvisoService avisoService;
    private final AvisoItemMapper avisoItemMapper;
    private final AccesoContenidoService accesoContenidoService;
    private final Clock clock;

    public List<AvisoItem> execute(ListarAvisosRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        var avisos = avisoService.listar(negocioId, request.estado());
        var acceso = accesoContenidoService.seleccionar(negocioId, TipoContenidoPlan.AVISOS,
                avisos.stream().map(aviso -> "servidor:" + aviso.getId()).toList(), OffsetDateTime.now(clock));
        return avisoItemMapper.toResponse(avisos.stream()
                .filter(aviso -> acceso.autorizadas().contains("servidor:" + aviso.getId())).toList());
    }
}
