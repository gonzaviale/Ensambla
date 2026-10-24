package com.ensambla.backend.domain.useCase.receta.obtenerReceta;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ObtenerReceta {

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    private final RegistroDatosService registroDatosService;

    private final RecetaService recetaService;

    private final RecetaRepository recetaRepository;

    public RecetaItem execute(ObtenerRecetaRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro producto = registroDatosService.obtener(request.registroId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.RECETAS_COSTOS, OffsetDateTime.now(clock));
        Receta receta = obtener(producto.getId(), negocioId);
        RecetaCosteada costeada = recetaService.costear(receta, negocioId);
        return RecetaItem.de(costeada);
    }

    /** La receta del producto; si todavía no tiene, una receta vacía. */
    private Receta obtener(RegistroId productoId, NegocioId negocioId) {
        return recetaRepository.findByProductoIdAndNegocioId(productoId, negocioId)
                .orElseGet(() -> Receta.builder()
                        .negocioId(negocioId)
                        .productoId(productoId)
                        .lineas(List.of())
                        .build());
    }
}
