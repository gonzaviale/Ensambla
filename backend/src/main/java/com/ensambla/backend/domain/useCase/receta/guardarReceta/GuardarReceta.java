package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GuardarReceta {

    private static final String MENSAJE_INSUMO_INEXISTENTE = "Ese insumo ya no existe";

    private final NegocioService negocioService;

    private final ControlPlanService controlPlanService;

    private final RegistroDatosService registroDatosService;

    private final RecetaService recetaService;

    private final RecetaRepository recetaRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public RecetaItem execute(GuardarRecetaRequest request) {
        NegocioId negocioId = negocioService.obtenerDelUsuario(request.usuarioId());
        Registro producto = registroDatosService.obtener(request.registroId(), negocioId);
        controlPlanService.validarFuncion(negocioId, FuncionPlan.RECETAS_COSTOS, OffsetDateTime.now(clock));
        Receta nueva = Receta.nueva(
                negocioId,
                producto.getId(),
                request.campoPrecioId(),
                request.aLineasReceta(),
                OffsetDateTime.now(clock));
        Receta guardada = transaccionPort.ejecutar(() -> guardar(nueva));
        RecetaCosteada costeada = recetaService.costear(guardada, negocioId);
        return RecetaItem.de(costeada);
    }

    /**
     * Guarda la receta ya armada (reemplaza la anterior del producto); una receta sin líneas quita la que hubiera. La
     * columna del precio tiene que ser numérica y cada insumo, una fila de su tabla.
     *
     * @throws RegistroInvalidoException si la columna del precio no sirve o algún insumo no existe en su tabla
     */
    private Receta guardar(Receta receta) {
        if (receta.esVacia()) {
            recetaRepository.deleteByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId());
            return receta;
        }
        Campo precio = campoRepository.findByIdAndNegocioId(receta.getCampoPrecioId(), receta.getNegocioId())
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .orElseThrow(() -> new RegistroInvalidoException(
                        Map.of("campoPrecioId", "Elegí una columna numérica con el precio de los insumos")));
        validarInsumos(receta, precio);
        recetaRepository.findByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId())
                .ifPresent(receta::reemplazar);
        return recetaRepository.save(receta);
    }

    private void validarInsumos(Receta receta, Campo precio) {
        Set<RegistroId> existentes = registroRepository.findAllByIdsAndBloque(
                        receta.insumoIds(), precio.getBloqueId(), receta.getNegocioId()).stream()
                .map(Registro::getId)
                .collect(Collectors.toSet());
        Map<String, String> errores = new LinkedHashMap<>();
        for (int indice = 0; indice < receta.getLineas().size(); indice++) {
            if (!existentes.contains(receta.getLineas().get(indice).insumoId())) {
                errores.put("lineas[" + indice + "]", MENSAJE_INSUMO_INEXISTENTE);
            }
        }
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }
}
