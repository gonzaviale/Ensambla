package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GuardarReceta {

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final RegistroDatosService registroDatosService;

    private final RecetaService recetaService;

    private final Clock clock;

    public RecetaItem execute(GuardarRecetaRequest request) {
        UUID negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro producto = registroDatosService.obtener(request.registroId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.RECETAS_COSTOS, OffsetDateTime.now(clock));
        Receta nueva = Receta.nueva(
                negocioId,
                producto.getId(),
                request.campoPrecioId(),
                request.aLineasReceta(),
                OffsetDateTime.now(clock));
        Receta guardada = recetaService.guardar(nueva);
        RecetaCosteada costeada = recetaService.costear(guardada, negocioId);
        return RecetaItem.de(costeada);
    }
}
