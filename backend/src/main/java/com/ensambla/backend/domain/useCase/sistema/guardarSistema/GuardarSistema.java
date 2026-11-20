package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.catalogo.CodigosInventario;
import com.ensambla.backend.domain.model.bloque.EstructuraFija;
import com.ensambla.backend.domain.model.bloque.ProteccionDeEstructura;
import com.ensambla.backend.domain.model.caja.ProteccionCaja;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.sistema.SistemaItem;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GuardarSistema {

    private final NegocioRepository negocioRepository;

    private final SistemaRepository sistemaRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public SistemaItem execute(GuardarSistemaRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Sistema sistema = request.sistema().aSistema(negocioId, request.id(), OffsetDateTime.now(clock));
        return SistemaItem.de(guardarEnTransaccion(negocioId, sistema, request.leidoEn()));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Sistema guardarEnTransaccion(NegocioId negocioId, Sistema sistema, OffsetDateTime leidoEn) {
        return transaccionPort.ejecutar(() -> guardar(negocioId, sistema, leidoEn));
    }

    /** Crea el sistema o lo reemplaza; si el id es de un proyecto de otro negocio responde como si no existiera. */
    private Sistema guardar(NegocioId negocioId, Sistema propuesto, OffsetDateTime leidoEn) {
        bloquearNegocio(negocioId);
        Optional<NegocioId> duenioActual = obtenerDuenioActual(propuesto);
        validarDuenio(negocioId, duenioActual);
        List<Sistema> actuales = obtenerSistemasDelNegocio(negocioId);
        validarProteccionDeCaja(actuales, propuesto, leidoEn);
        validarProteccionDeEstructura(actuales, propuesto);
        Sistema sistema = completarLaEstructura(propuesto);
        validarCodigosDeInventario(sistema);
        validarAltaDelProyecto(negocioId, sistema, duenioActual);
        validarRegistrosNuevos(negocioId, sistema);
        guardarSistema(sistema, leidoEn);
        return sistema;
    }

    private void validarDuenio(NegocioId negocioId, Optional<NegocioId> duenioActual) {
        duenioActual
                .filter(duenio -> !duenio.equals(negocioId))
                .ifPresent(duenio -> {
                    throw new ResourceNotFoundException("Sistema no encontrado");
                });
    }

    private void validarProteccionDeCaja(List<Sistema> actuales, Sistema propuesto, OffsetDateTime leidoEn) {
        ProteccionCaja.validar(actuales, propuesto, leidoEn);
    }

    private void validarProteccionDeEstructura(List<Sistema> actuales, Sistema propuesto) {
        ProteccionDeEstructura.validar(actuales, propuesto);
    }

    private Sistema completarLaEstructura(Sistema propuesto) {
        return EstructuraFija.completar(propuesto, OffsetDateTime.now(clock));
    }

    private void validarCodigosDeInventario(Sistema sistema) {
        sistema.proyecto().getBloques().forEach(bloque -> CodigosInventario.validar(
                bloque, sistema.registrosPorBloque().getOrDefault(bloque.getId(), List.of())));
    }

    private void validarAltaDelProyecto(NegocioId negocioId, Sistema sistema, Optional<NegocioId> duenioActual) {
        if (duenioActual.isEmpty()) {
            validarCupoDeProyectos(negocioId, sistema);
        }
    }

    /** Sólo cuenta contra el plan lo que el reemplazo suma respecto de lo que ya había. */
    private void validarRegistrosNuevos(NegocioId negocioId, Sistema sistema) {
        long cantidad = sistema.registrosPorBloque().values().stream().mapToLong(List::size).sum();
        long anteriores = contarRegistrosAnteriores(negocioId, sistema);
        if (cantidad > anteriores) {
            validarCupoDeRegistros(negocioId, cantidad - anteriores, sistema);
        }
    }

    private void bloquearNegocio(NegocioId negocioId) {
        usoPlanRepository.bloquearNegocio(negocioId);
    }

    private Optional<NegocioId> obtenerDuenioActual(Sistema sistema) {
        return sistemaRepository.findNegocioIdByProyectoId(sistema.id());
    }

    private List<Sistema> obtenerSistemasDelNegocio(NegocioId negocioId) {
        return sistemaRepository.findAllByNegocioId(negocioId);
    }

    private long contarRegistrosAnteriores(NegocioId negocioId, Sistema sistema) {
        return usoPlanRepository.contarRegistrosProyecto(negocioId, sistema.id());
    }

    private void validarCupoDeProyectos(NegocioId negocioId, Sistema sistema) {
        controlPlanService.validarAltaProyecto(negocioId, sistema.proyecto().getActualizadoEn());
    }

    private void validarCupoDeRegistros(NegocioId negocioId, long cantidad, Sistema sistema) {
        controlPlanService.validarAltaRegistros(negocioId, cantidad, sistema.proyecto().getActualizadoEn());
    }

    private void guardarSistema(Sistema sistema, OffsetDateTime leidoEn) {
        sistemaRepository.guardar(sistema, leidoEn);
    }
}
