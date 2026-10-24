package com.ensambla.backend.domain.useCase.receta;

import com.ensambla.backend.domain.dto.receta.ObtenerRecetaRequest;
import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.mapper.receta.ObtenerRecetaMapper;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerRecetaUseCase {

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    private final RegistroDatosService registroDatosService;

    private final RecetaService recetaService;

    private final ObtenerRecetaMapper obtenerRecetaMapper;

    public RecetaItem execute(ObtenerRecetaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro producto = registroDatosService.obtener(request.registroId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.RECETAS_COSTOS, OffsetDateTime.now(clock));
        Receta receta = recetaService.obtener(producto.getId(), negocioId);
        RecetaCosteada costeada = recetaService.costear(receta, negocioId);
        return obtenerRecetaMapper.toResponse(costeada);
    }
}
