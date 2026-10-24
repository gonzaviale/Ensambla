package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ejecuta la acción sugerida de un aviso. Sólo {@code EDITAR_REGISTRO} cambia datos (por ejemplo, el precio de un
 * producto); las demás ({@code REGISTRAR}, {@code ABRIR}) son atajos de pantalla y no tocan nada.
 */
@RequiredArgsConstructor
public class AccionAvisoService {

    static final String EDITAR_REGISTRO = "EDITAR_REGISTRO";

    private final RegistroDatosService registroDatosService;

    private final CampoService campoService;

    private final TransaccionPort transaccionPort;

    /** Debe correr dentro de la transacción que marca el aviso como aplicado, para que sea una sola operación. */
    public void ejecutar(Aviso aviso, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            aviso.getAcciones().stream()
                    .filter(Map.class::isInstance)
                    .map(accion -> (Map<?, ?>) accion)
                    .filter(accion -> EDITAR_REGISTRO.equals(accion.get("tipo")))
                    .findFirst()
                    .ifPresent(accion -> editarRegistro(aviso.getNegocioId(), accion, ahora));
        });
    }

    private void editarRegistro(UUID negocioId, Map<?, ?> accion, OffsetDateTime ahora) {
        Registro registro = registroDatosService.obtener(registroIdDe(accion), negocioId);
        List<Campo> campos = campoService.listarDeBloque(registro.getBloqueId(), negocioId);
        registroDatosService.editar(registro, campos, cambiosPorCampo(accion, campos), ahora);
    }

    private static UUID registroIdDe(Map<?, ?> accion) {
        try {
            return UUID.fromString(String.valueOf(accion.get("registroId")));
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
