package com.ensambla.backend.domain.useCase.aviso.aplicarAviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.AccionCrearRegistro;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionEditarRegistro;
import com.ensambla.backend.domain.model.aviso.AplicadorDeAcciones;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.objetivo.DecisionesObjetivoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.aviso.AvisoItem;
import com.ensambla.backend.domain.useCase.aviso.ResolverAvisoRequest;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ejecuta la acción sugerida del aviso y lo marca como aplicado en una sola operación. Las acciones que cambian datos
 * (editar una fila, crear una) se aplican a sí mismas; las demás son atajos de pantalla y no tocan nada.
 */
@RequiredArgsConstructor
public class AplicarAviso {

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final RegistroDatosService registroDatosService;

    private final RegistroRepository registroRepository;

    private final CampoRepository campoRepository;

    private final AvisoRepository avisoRepository;

    private final DecisionesObjetivoService decisionesObjetivoService;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        NegocioId negocioId = obtenerNegocioDelUsuario(request.usuarioId());
        Aviso aviso = obtenerAviso(request.avisoId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        exigirAccesoAlAviso(negocioId, aviso, ahora);
        return AvisoItem.de(aplicarEnTransaccion(aviso, ahora));
    }

    private NegocioId obtenerNegocioDelUsuario(UsuarioId usuarioId) {
        return negocioRepository.obtenerIdDelUsuario(usuarioId);
    }

    private Aviso obtenerAviso(AvisoId avisoId, NegocioId negocioId) {
        return avisoRepository.obtener(avisoId, negocioId);
    }

    private void exigirAccesoAlAviso(NegocioId negocioId, Aviso aviso, OffsetDateTime ahora) {
        controlPlanService.exigirAccesoAlAviso(negocioId, aviso.getId(), ahora);
    }

    private Aviso aplicarEnTransaccion(Aviso aviso, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> aplicar(aviso, ahora));
    }

    private Aviso aplicar(Aviso aviso, OffsetDateTime ahora) {
        aviso.aplicar(ahora);
        ejecutarAccionQueCambiaDatos(aviso, ahora);
        Aviso aplicado = guardar(aviso);
        registrarDecision(aplicado, ahora);
        return aplicado;
    }

    private void ejecutarAccionQueCambiaDatos(Aviso aviso, OffsetDateTime ahora) {
        AplicadorDeAcciones aplicador = new AplicadorDelNegocio(aviso.getNegocioId(), ahora);
        aviso.getAcciones().stream()
                .filter(AccionDeAviso::modificaDatos)
                .findFirst()
                .ifPresent(accion -> accion.aplicarEn(aplicador));
    }

    private Aviso guardar(Aviso aviso) {
        return avisoRepository.save(aviso);
    }

    private void registrarDecision(Aviso aplicado, OffsetDateTime ahora) {
        decisionesObjetivoService.registrarAviso(aplicado, TipoDecision.AVISO_APLICADO, ahora);
    }

    /** Suma la fila que el aviso propone (un combo) con las mismas validaciones y límites del plan que cargarla a mano. */
    private void crearRegistro(NegocioId negocioId, AccionCrearRegistro accion, OffsetDateTime ahora) {
        List<Campo> campos = obtenerCamposDe(accion.bloqueId(), negocioId);
        Map<String, Object> validos = Registro.validar(campos, cambiosPorCampo(accion.valores(), campos));
        validarConexiones(negocioId, campos, validos);
        validarCupoDeRegistros(negocioId, ahora);
        guardarRegistro(Registro.deAviso(negocioId, accion.bloqueId(), validos, ahora));
    }

    private void editarRegistro(NegocioId negocioId, AccionEditarRegistro accion, OffsetDateTime ahora) {
        Registro registro = obtenerRegistro(accion.registroId(), negocioId);
        List<Campo> campos = obtenerCamposDe(registro.getBloqueId(), negocioId);
        registroDatosService.editar(registro, campos, cambiosPorCampo(accion.valores(), campos), ahora);
    }

    private List<Campo> obtenerCamposDe(BloqueId bloqueId, NegocioId negocioId) {
        return campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
    }

    private Registro obtenerRegistro(RegistroId registroId, NegocioId negocioId) {
        return registroRepository.obtener(registroId, negocioId);
    }

    private void validarConexiones(NegocioId negocioId, List<Campo> campos, Map<String, Object> valores) {
        registroDatosService.validarConexiones(negocioId, campos, valores);
    }

    private void validarCupoDeRegistros(NegocioId negocioId, OffsetDateTime ahora) {
        controlPlanService.validarAltaRegistros(negocioId, 1, ahora);
    }

    private void guardarRegistro(Registro registro) {
        registroRepository.save(registro);
    }

    /** Los valores de la acción vienen por clave semántica ({@code price}); el registro los guarda por id de campo. */
    private static Map<String, Object> cambiosPorCampo(Map<String, Object> valores, List<Campo> campos) {
        Map<String, Object> cambios = new LinkedHashMap<>();
        valores.forEach((clave, valor) -> campoDeClave(campos, clave)
                .ifPresent(campo -> cambios.put(campo.getId().toString(), valor)));
        if (cambios.isEmpty()) {
            throw new BusinessRuleException("La sección ya no tiene los datos que el aviso quería cambiar");
        }
        return cambios;
    }

    private static Optional<Campo> campoDeClave(List<Campo> campos, String clave) {
        return campos.stream().filter(campo -> clave.equals(campo.getClave())).findFirst();
    }

    private final class AplicadorDelNegocio implements AplicadorDeAcciones {

        private final NegocioId negocioId;

        private final OffsetDateTime ahora;

        private AplicadorDelNegocio(NegocioId negocioId, OffsetDateTime ahora) {
            this.negocioId = negocioId;
            this.ahora = ahora;
        }

        @Override
        public void editar(AccionEditarRegistro accion) {
            editarRegistro(negocioId, accion, ahora);
        }

        @Override
        public void crear(AccionCrearRegistro accion) {
            crearRegistro(negocioId, accion, ahora);
        }
    }
}
