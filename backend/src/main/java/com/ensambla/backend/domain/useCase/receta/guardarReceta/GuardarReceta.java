package com.ensambla.backend.domain.useCase.receta.guardarReceta;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.receta.RecetaRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.useCase.receta.RecetaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GuardarReceta {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final RecetaService recetaService;

    private final RecetaRepository recetaRepository;

    private final CampoRepository campoRepository;

    private final RegistroRepository registroRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public RecetaItem execute(GuardarRecetaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Registro producto = obtenerProducto(request.registroId(), negocioId);
        validarQueElPlanIncluyaRecetas(negocioId);
        Receta nueva = armarReceta(negocioId, producto, request);
        Receta guardada = guardarEnTransaccion(nueva);
        return RecetaItem.de(costear(guardada, negocioId));
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

    private Receta armarReceta(NegocioId negocioId, Registro producto, GuardarRecetaRequest request) {
        return Receta.nueva(
                negocioId,
                producto.getId(),
                request.campoPrecioId(),
                request.aLineasReceta(),
                OffsetDateTime.now(clock));
    }

    private Receta guardarEnTransaccion(Receta receta) {
        return transaccionPort.ejecutar(() -> guardar(receta));
    }

    private RecetaCosteada costear(Receta receta, NegocioId negocioId) {
        return recetaService.costear(receta, negocioId);
    }

    /** Guarda la receta ya armada (reemplaza la anterior del producto); una receta sin líneas quita la que hubiera. */
    private Receta guardar(Receta receta) {
        if (receta.esVacia()) {
            return quitar(receta);
        }
        Campo precio = obtenerColumnaDelPrecio(receta);
        validarInsumos(receta, precio);
        conservarLaIdentidadDeLaAnterior(receta);
        return guardarReceta(receta);
    }

    private Receta quitar(Receta receta) {
        eliminarRecetaDelProducto(receta);
        return receta;
    }

    /** La columna del precio tiene que existir y ser numérica. */
    private Campo obtenerColumnaDelPrecio(Receta receta) {
        return buscarCampo(receta.getCampoPrecioId(), receta.getNegocioId())
                .filter(this::esNumerico)
                .orElseThrow(() -> new RegistroInvalidoException(
                        Map.of("campoPrecioId", "Elegí una columna numérica con el precio de los insumos")));
    }

    private boolean esNumerico(Campo campo) {
        return campo.getTipo() == TipoCampo.NUMERO;
    }

    private void validarInsumos(Receta receta, Campo precio) {
        Set<RegistroId> existentes = idsDeInsumosExistentes(receta, precio);
        Map<String, String> errores = receta.erroresDeInsumosInexistentes(existentes);
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }

    private Set<RegistroId> idsDeInsumosExistentes(Receta receta, Campo precio) {
        return buscarInsumos(receta, precio).stream()
                .map(Registro::getId)
                .collect(Collectors.toSet());
    }

    private void conservarLaIdentidadDeLaAnterior(Receta receta) {
        buscarRecetaDelProducto(receta).ifPresent(receta::reemplazar);
    }

    private Optional<Campo> buscarCampo(CampoId campoId, NegocioId negocioId) {
        return campoRepository.findByIdAndNegocioId(campoId, negocioId);
    }

    private List<Registro> buscarInsumos(Receta receta, Campo precio) {
        return registroRepository.findAllByIdsAndBloque(
                receta.insumoIds(), precio.getBloqueId(), receta.getNegocioId());
    }

    private Optional<Receta> buscarRecetaDelProducto(Receta receta) {
        return recetaRepository.findByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId());
    }

    private void eliminarRecetaDelProducto(Receta receta) {
        recetaRepository.deleteByProductoIdAndNegocioId(receta.getProductoId(), receta.getNegocioId());
    }

    private Receta guardarReceta(Receta receta) {
        return recetaRepository.save(receta);
    }
}
