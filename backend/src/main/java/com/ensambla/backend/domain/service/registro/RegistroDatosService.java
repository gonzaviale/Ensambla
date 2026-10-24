package com.ensambla.backend.domain.service.registro;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.registro.ConsultaRegistros;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RegistroDatosService {

    static final int MAXIMO_REFERENCIAS = 20;

    private static final String MENSAJE_FILA_INEXISTENTE = "Esa fila ya no existe";

    private final RegistroRepository registroRepository;

    private final ControlPlanService controlPlanService;

    private final TransaccionPort transaccionPort;

    public Registro crear(UUID negocioId, UUID bloqueId, List<Campo> campos, Map<String, Object> valores,
                          OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            Map<String, Object> validos = Registro.validar(campos, valores);
            validarConexiones(negocioId, campos, validos);
            controlPlanService.validarAltaRegistros(negocioId, 1, ahora);
            return registroRepository.save(Registro.nuevo(negocioId, bloqueId, validos, ahora));
        });
    }

    /** Guarda registros ya validados (por ejemplo, los que salen de una lectura del cuaderno). */
    public List<Registro> crearVarios(UUID negocioId, List<Registro> registros, OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            if (registros.stream().anyMatch(registro -> !negocioId.equals(registro.getNegocioId()))) {
                throw new BusinessRuleException("Los registros deben pertenecer al negocio de la sesión");
            }
            controlPlanService.validarAltaRegistros(negocioId, registros.size(), ahora);
            return registroRepository.saveAll(registros);
        });
    }

    public Registro obtener(UUID registroId, UUID negocioId) {
        return registroRepository.findByIdAndNegocioId(registroId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El registro no existe"));
    }

    /** Las filas del bloque con esos ids, las más nuevas primero; las que no existen no vienen. */
    public List<Registro> obtenerVarios(Collection<UUID> ids, UUID bloqueId, UUID negocioId) {
        return registroRepository.findAllByIdsAndBloque(ids, bloqueId, negocioId);
    }

    /**
     * Filas del bloque que se ofrecen en un campo RELACION, las más nuevas primero: con {@code ids}, sólo esas (las que
     * existen); sin ids, hasta {@value #MAXIMO_REFERENCIAS} que coinciden con el texto como en el listado.
     */
    public List<Registro> buscarReferenciables(UUID negocioId, UUID bloqueId, List<Campo> campos, String texto,
                                               List<UUID> ids) {
        if (!ids.isEmpty()) {
            return registroRepository.findAllByIdsAndBloque(ids, bloqueId, negocioId);
        }
        ConsultaRegistros consulta = ConsultaRegistros.armar(
                negocioId, bloqueId, campos, texto, null, null, 0, MAXIMO_REFERENCIAS);
        return registroRepository.buscar(consulta).contenido();
    }

    public Pagina<Registro> listar(ConsultaRegistros consulta) {
        return registroRepository.buscar(consulta);
    }

    /** Cantidad y suma de cada campo numérico de los registros que cumplen la consulta, sin importar la página. */
    public TotalesRegistros totales(ConsultaRegistros consulta, List<Campo> campos, long cantidad) {
        List<UUID> numericos = campos.stream()
                .filter(campo -> campo.getTipo() == TipoCampo.NUMERO)
                .map(Campo::getId)
                .toList();
        return TotalesRegistros.armar(cantidad, numericos, registroRepository.sumarNumericos(consulta, numericos));
    }

    public Registro editar(Registro registro, List<Campo> campos, Map<String, Object> cambios,
                           OffsetDateTime ahora) {
        return transaccionPort.ejecutar(() -> {
            Map<String, Object> validos = Registro.validar(campos, registro.combinarCon(cambios));
            validarConexiones(registro.getNegocioId(), campos, validos);
            registro.reemplazarValores(validos, ahora);
            return registroRepository.save(registro);
        });
    }

    public void eliminar(Registro registro) {
        transaccionPort.ejecutar(() -> {
            registroRepository.deleteById(registro.getId());
        });
    }

    /**
     * Cada valor de un campo RELACION tiene que ser una fila existente de la tabla conectada. Hace una consulta por
     * tabla conectada, no una por valor. Los valores ya vienen validados como ids.
     *
     * @throws RegistroInvalidoException con "Esa fila ya no existe" en cada campo que apunta a una fila inexistente
     */
    public void validarConexiones(UUID negocioId, List<Campo> campos, Map<String, Object> valores) {
        Map<UUID, List<Campo>> porBloque = campos.stream()
                .filter(Campo::esRelacion)
                .filter(campo -> valores.containsKey(campo.getId().toString()))
                .collect(Collectors.groupingBy(Campo::getBloqueRelacionadoId));
        Map<String, String> errores = new LinkedHashMap<>();
        porBloque.forEach((bloqueId, conexiones) -> {
            Set<UUID> buscados = conexiones.stream()
                    .map(campo -> referenciaDe(campo, valores))
                    .collect(Collectors.toSet());
            Set<UUID> existentes = registroRepository.findAllByIdsAndBloque(buscados, bloqueId, negocioId).stream()
                    .map(Registro::getId)
                    .collect(Collectors.toSet());
            conexiones.stream()
                    .filter(campo -> !existentes.contains(referenciaDe(campo, valores)))
                    .forEach(campo -> errores.put(campo.getId().toString(), MENSAJE_FILA_INEXISTENTE));
        });
        if (!errores.isEmpty()) {
            throw new RegistroInvalidoException(errores);
        }
    }

    private static UUID referenciaDe(Campo campo, Map<String, Object> valores) {
        return UUID.fromString((String) valores.get(campo.getId().toString()));
    }
}
