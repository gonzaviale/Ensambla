package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ejecuta la acción sugerida de un aviso. Sólo cambian datos {@code EDITAR_REGISTRO} (por ejemplo, el precio de un
 * producto) y {@code CREAR_REGISTRO} (por ejemplo, el combo de dos productos); las demás ({@code REGISTRAR},
 * {@code ABRIR}) son atajos de pantalla y no tocan nada. Los valores de la acción vienen por clave semántica.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccionAvisoService {

    static final String EDITAR_REGISTRO = "EDITAR_REGISTRO";
    static final String CREAR_REGISTRO = "CREAR_REGISTRO";

    private static final Set<String> ACCIONES_CON_DATOS = Set.of(EDITAR_REGISTRO, CREAR_REGISTRO);

    private final RegistroDatosService registroDatosService;

    private final CampoService campoService;

    /** Debe correr dentro de la transacción que marca el aviso como aplicado, para que sea una sola operación. */
    @Transactional
    public void ejecutar(Aviso aviso, OffsetDateTime ahora) {
        aviso.getAcciones().stream()
                .filter(Map.class::isInstance)
                .map(accion -> (Map<?, ?>) accion)
                .filter(accion -> accion.get("tipo") instanceof String tipo && ACCIONES_CON_DATOS.contains(tipo))
                .findFirst()
                .ifPresent(accion -> ejecutarAccion(aviso.getNegocioId(), accion, ahora));
    }

    private void ejecutarAccion(UUID negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        if (EDITAR_REGISTRO.equals(accion.get("tipo"))) {
            editarRegistro(negocioId, accion, ahora);
        } else {
            crearRegistro(negocioId, accion, ahora);
        }
    }

    private void editarRegistro(UUID negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        Registro registro = registroDatosService.obtener(
                idDe(accion, "registroId", "La acción del aviso no indica qué registro cambiar"), negocioId);
        List<Campo> campos = campoService.listarDeBloque(registro.getBloqueId(), negocioId);
        registroDatosService.editar(registro, campos, cambiosPorCampo(accion, campos), ahora);
    }

    /** Una tabla de otro negocio no devuelve campos, así que se rechaza igual que una tabla que ya no existe. */
    private void crearRegistro(UUID negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        UUID bloqueId = idDe(accion, "bloqueId", "La acción del aviso no indica en qué tabla crear");
        List<Campo> campos = campoService.listarDeBloque(bloqueId, negocioId);
        if (campos.isEmpty()) {
            throw new BusinessRuleException("La tabla del aviso ya no existe");
        }
        registroDatosService.crear(negocioId, bloqueId, campos, cambiosPorCampo(accion, campos), ahora);
    }

    private static UUID idDe(Map<?, ?> accion, String clave, String mensaje) {
        try {
            return UUID.fromString(String.valueOf(accion.get(clave)));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(mensaje);
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
