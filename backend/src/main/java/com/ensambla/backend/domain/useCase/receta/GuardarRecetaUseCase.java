package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.dto.receta.GuardarRecetaRequest;
import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.mapper.receta.GuardarRecetaMapper;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GuardarRecetaUseCase {

    private final NegocioService negocioService;

    private final RegistroDatosService registroDatosService;

    private final RecetaService recetaService;

    private final GuardarRecetaMapper guardarRecetaMapper;

    private final Clock clock;

    public RecetaItem execute(GuardarRecetaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro producto = registroDatosService.obtener(request.registroId(), negocioId);
        Receta nueva = Receta.nueva(
                negocioId,
                producto.getId(),
                request.campoPrecioId(),
                guardarRecetaMapper.toLineas(request),
                OffsetDateTime.now(clock));
        Receta guardada = recetaService.guardar(nueva);
        RecetaCosteada costeada = recetaService.costear(guardada, negocioId);
        return guardarRecetaMapper.toResponse(costeada);
    }
}
