package com.ensambla.backend.domain.useCase.receta.obtenerReceta;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class ObtenerReceta {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final Clock clock;

    private final RegistroRepository registroRepository;

    private final RecetaService recetaService;

    private final RecetaRepository recetaRepository;

    public RecetaItem execute(ObtenerRecetaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro producto = obtenerProducto(request.registroId(), negocioId);
        validarQueElPlanIncluyaRecetas(negocioId);
        Receta receta = obtenerRecetaDelProducto(producto.getId(), negocioId);
        return RecetaItem.de(costear(receta, negocioId));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Registro obtenerProducto(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private void validarQueElPlanIncluyaRecetas(NegocioId negocioId) {
        controlPlanService.validarFuncion(negocioId, FuncionPlan.RECETAS_COSTOS, OffsetDateTime.now(clock));
    }

    /** La receta del producto; si todavía no tiene, una receta vacía. */
    private Receta obtenerRecetaDelProducto(RegistroId productoId, NegocioId negocioId) {
        return recetaRepository.findByProductoIdAndNegocioId(productoId, negocioId)
                .orElseGet(() -> Receta.vacia(negocioId, productoId));
    }

    private RecetaCosteada costear(Receta receta, NegocioId negocioId) {
        return recetaService.costear(receta, negocioId);
    }
}
