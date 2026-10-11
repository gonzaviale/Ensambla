package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.mapper.campo.EditarCampoMapper;
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
public class EditarCampoUseCase {

    private final NegocioService negocioService;

    private final CampoService campoService;

    private final BloqueService bloqueService;

    private final EditarCampoMapper editarCampoMapper;

    private final Clock clock;

    public CampoItem execute(EditarCampoRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Campo campo = campoService.obtener(request.campoId(), negocioId);
        Bloque bloque = bloqueService.obtener(campo.getBloqueId(), negocioId);
        ConfiguracionCampo configuracion = editarCampoMapper.toConfiguracion(request);
        Campo editado = campoService.editar(bloque, campo, configuracion, OffsetDateTime.now(clock));
        return editarCampoMapper.toResponse(editado);
    }
}
