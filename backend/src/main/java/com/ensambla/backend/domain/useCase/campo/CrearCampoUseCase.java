package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.CrearCampoRequest;
import com.ensambla.backend.domain.mapper.campo.CrearCampoMapper;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCampoUseCase {

    private final NegocioService negocioService;

    private final BloqueService bloqueService;

    private final CampoService campoService;

    private final CrearCampoMapper crearCampoMapper;

    private final Clock clock;

    public CampoItem execute(CrearCampoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Bloque bloque = bloqueService.obtener(request.bloqueId(), negocioId);
        ConfiguracionCampo configuracion = crearCampoMapper.toConfiguracion(request);
        Campo campo = campoService.crear(bloque, configuracion, OffsetDateTime.now(clock));
        return crearCampoMapper.toResponse(campo);
    }
}
