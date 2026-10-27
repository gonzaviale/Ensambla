package com.ensambla.backend.domain.useCase.aviso.aplicarAviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
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
import java.util.Set;

/**
 * Ejecuta la acción sugerida del aviso y lo marca como aplicado en una sola operación. Sólo cambian datos
 * {@code EDITAR_REGISTRO} (por ejemplo, el precio de un producto) y {@code CREAR_REGISTRO} (por ejemplo, el combo de
 * dos productos); las demás ({@code REGISTRAR}, {@code ABRIR}) son atajos de pantalla y no tocan nada. Los valores de
 * la acción vienen por clave semántica.
 */
@RequiredArgsConstructor
public class AplicarAviso {

    static final String EDITAR_REGISTRO = "EDITAR_REGISTRO";
    static final String CREAR_REGISTRO = "CREAR_REGISTRO";
    private static final Set<String> ACCIONES_CON_DATOS = Set.of(EDITAR_REGISTRO, CREAR_REGISTRO);

    private final NegocioRepository negocioRepository;

    private final ControlPlanService controlPlanService;

    private final RegistroDatosService registroDatosService;

    private final RegistroRepository registroRepository;

    private final CampoRepository campoRepository;

    private final AvisoRepository avisoRepository;

    private final TransaccionPort transaccionPort;

    private final Clock clock;

    public AvisoItem execute(ResolverAvisoRequest request) {
        NegocioId negocioId = negocioRepository.obtenerIdDelUsuario(request.usuarioId());
        Aviso aviso = avisoRepository.obtener(request.avisoId(), negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        controlPlanService.exigir(negocioId, TipoContenidoPlan.AVISOS, "servidor:" + aviso.getId(), ahora);
        return AvisoItem.de(transaccionPort.ejecutar(() -> aplicar(aviso, ahora)));
    }

    private Aviso aplicar(Aviso aviso, OffsetDateTime ahora) {
        aviso.aplicar(ahora);
        ejecutarAccion(aviso, ahora);
        return avisoRepository.save(aviso);
    }

    private void ejecutarAccion(Aviso aviso, OffsetDateTime ahora) {
        aviso.getAcciones().stream()
                .filter(Map.class::isInstance)
                .map(accion -> (Map<?, ?>) accion)
                .filter(accion -> accion.get("tipo") instanceof String tipo && ACCIONES_CON_DATOS.contains(tipo))
                .findFirst()
                .ifPresent(accion -> ejecutarAccionConDatos(aviso.getNegocioId(), accion, ahora));
    }

    private void ejecutarAccionConDatos(NegocioId negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        if (EDITAR_REGISTRO.equals(accion.get("tipo"))) {
            editarRegistro(negocioId, accion, ahora);
        } else {
            crearRegistro(negocioId, accion, ahora);
        }
    }

    private void editarRegistro(NegocioId negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        Registro registro = registroRepository.obtener(registroIdDe(accion), negocioId);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(registro.getBloqueId(), negocioId);
        registroDatosService.editar(registro, campos, cambiosPorCampo(accion, campos), ahora);
    }

    /**
     * Crea la fila con las mismas comprobaciones que una carga manual (valores, conexiones y cupo del plan). Una tabla de
     * otro negocio no devuelve campos, así que se rechaza igual que una tabla que ya no existe.
     */
    private void crearRegistro(NegocioId negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        BloqueId bloqueId = bloqueIdDe(accion);
        List<Campo> campos = campoRepository.findByBloqueIdAndNegocioId(bloqueId, negocioId);
        if (campos.isEmpty()) {
            throw new BusinessRuleException("La tabla del aviso ya no existe");
        }
        Map<String, Object> validos = Registro.validar(campos, cambiosPorCampo(accion, campos));
        registroDatosService.validarConexiones(negocioId, campos, validos);
        controlPlanService.validarAltaRegistros(negocioId, 1, ahora);
        registroRepository.save(Registro.nuevo(negocioId, bloqueId, validos, ahora));
    }

    private static BloqueId bloqueIdDe(Map<?, ?> accion) {
        try {
            return BloqueId.de(String.valueOf(accion.get("bloqueId")));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("La acción del aviso no indica en qué tabla crear");
        }
    }

    private static RegistroId registroIdDe(Map<?, ?> accion) {
        try {
            return RegistroId.de(String.valueOf(accion.get("registroId")));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("La acción del aviso no indica qué registro cambiar");
        }
    }

    /** Los valores de la acción vienen por clave semántica ({@code price}); el registro los guarda por id de campo. */
    private static Map<String, Object> cambiosPorCampo(Map<?, ?> accion, List<Campo> campos) {
        if (!(accion.get("valores") instanceof Map<?, ?> valores) || valores.isEmpty()) {
            throw new BusinessRuleException("La acción del aviso no indica qué valores cambiar");
        }
        Map<String, Object> cambios = new LinkedHashMap<>();
        valores.forEach((clave, valor) -> campos.stream()
                .filter(campo -> clave.equals(campo.getClave()))
                .findFirst()
                .ifPresent(campo -> cambios.put(campo.getId().toString(), valor)));
        if (cambios.isEmpty()) {
            throw new BusinessRuleException("La tabla ya no tiene los campos que el aviso quería cambiar");
        }
        return cambios;
    }
}
